package studio.aroundhub.tunagiftset.product.service;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studio.aroundhub.tunagiftset.entity.Member;
import studio.aroundhub.tunagiftset.entity.Product;
import studio.aroundhub.tunagiftset.entity.Wishlist;
import studio.aroundhub.tunagiftset.entity.type.ProductStatus;
import studio.aroundhub.tunagiftset.exception.InvalidRequestException;
import studio.aroundhub.tunagiftset.exception.ResourceNotFoundException;
import studio.aroundhub.tunagiftset.product.dto.WishlistItemResponse;
import studio.aroundhub.tunagiftset.product.dto.WishlistPageResponse;
import studio.aroundhub.tunagiftset.product.dto.WishlistProductIdsResponse;
import studio.aroundhub.tunagiftset.product.dto.WishlistStatusResponse;
import studio.aroundhub.tunagiftset.repository.MemberRepository;
import studio.aroundhub.tunagiftset.repository.ProductImageRepository;
import studio.aroundhub.tunagiftset.repository.ProductRepository;
import studio.aroundhub.tunagiftset.repository.WishlistRepository;
import studio.aroundhub.tunagiftset.storage.ObjectStorageService;

@Service
@Transactional(readOnly = true)
public class WishlistService {

    private static final int MAX_PAGE_SIZE = 100;

    private final WishlistRepository wishlistRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ObjectStorageService objectStorageService;

    public WishlistService(
            WishlistRepository wishlistRepository,
            MemberRepository memberRepository,
            ProductRepository productRepository,
            ProductImageRepository productImageRepository,
            ObjectStorageService objectStorageService
    ) {
        this.wishlistRepository = wishlistRepository;
        this.memberRepository = memberRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.objectStorageService = objectStorageService;
    }

    @Transactional
    public synchronized WishlistStatusResponse add(Long memberId, Long productId) {
        Member member = getMember(memberId);
        Product product = getProduct(productId);
        validateWishable(product);

        if (wishlistRepository.existsByMemberIdAndProductId(memberId, productId)) {
            return status(memberId, productId);
        }

        try {
            wishlistRepository.saveAndFlush(new Wishlist(member, product));
        } catch (DataIntegrityViolationException ignored) {
            // The unique constraint is the final guard for duplicate concurrent POSTs.
        }

        return status(memberId, productId);
    }

    @Transactional
    public WishlistStatusResponse remove(Long memberId, Long productId) {
        getProduct(productId);
        wishlistRepository.findByMemberIdAndProductId(memberId, productId)
                .ifPresent(wishlistRepository::delete);
        return status(memberId, productId);
    }

    public WishlistStatusResponse status(Long memberId, Long productId) {
        getProduct(productId);
        boolean wishlisted = wishlistRepository.existsByMemberIdAndProductId(memberId, productId);
        long wishlistCount = wishlistRepository.countByProductId(productId);
        return new WishlistStatusResponse(productId, wishlisted, wishlistCount);
    }

    public WishlistProductIdsResponse findWishlistedProductIds(Long memberId, List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return new WishlistProductIdsResponse(List.of());
        }
        Set<Long> uniqueIds = new LinkedHashSet<>(productIds);
        return new WishlistProductIdsResponse(wishlistRepository.findWishlistedProductIds(memberId, uniqueIds));
    }

    public WishlistPageResponse findMyWishlist(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))
        );

        Page<Wishlist> wishlists = wishlistRepository.findPageByMemberIdExcludingProductStatus(
                memberId,
                ProductStatus.HIDDEN,
                pageable
        );
        Map<Long, String> thumbnails = findThumbnails(wishlists.getContent());
        Page<WishlistItemResponse> response = wishlists.map(wishlist ->
                WishlistItemResponse.from(wishlist, thumbnails.get(wishlist.getProduct().getId())));
        return WishlistPageResponse.from(response);
    }

    public Map<Long, Long> countByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> counts = new HashMap<>();
        wishlistRepository.countByProductIds(productIds)
                .forEach(projection -> counts.put(projection.getProductId(), projection.getWishlistCount()));
        return counts;
    }

    private Map<Long, String> findThumbnails(List<Wishlist> wishlists) {
        List<Long> productIds = wishlists.stream()
                .map(wishlist -> wishlist.getProduct().getId())
                .toList();
        if (productIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> thumbnails = new HashMap<>();
        productImageRepository.findMainThumbnailsByProductIds(productIds)
                .forEach(thumbnail -> thumbnails.putIfAbsent(
                        thumbnail.getProductId(),
                        resolveImageUrl(thumbnail.getObjectKey(), thumbnail.getImageUrl())
                ));
        return thumbnails;
    }

    private String resolveImageUrl(String objectKey, String fallbackUrl) {
        if (objectKey == null || objectKey.isBlank()) {
            return fallbackUrl;
        }
        return objectStorageService.publicUrl(objectKey);
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("MEMBER_NOT_FOUND", "Member not found."));
    }

    private Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("PRODUCT_NOT_FOUND", "Product not found."));
    }

    private void validateWishable(Product product) {
        if (product.getStatus() == ProductStatus.HIDDEN || product.getStatus() == ProductStatus.DISCONTINUED) {
            throw new InvalidRequestException("WISHLIST_NOT_ALLOWED", "This product cannot be added to wishlist.");
        }
    }
}
