package studio.aroundhub.tunagiftset.cart.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studio.aroundhub.tunagiftset.cart.dto.CartItemAddRequest;
import studio.aroundhub.tunagiftset.cart.dto.CartItemDeleteRequest;
import studio.aroundhub.tunagiftset.cart.dto.CartItemQuantityUpdateRequest;
import studio.aroundhub.tunagiftset.cart.dto.CartItemResponse;
import studio.aroundhub.tunagiftset.cart.dto.CartResponse;
import studio.aroundhub.tunagiftset.cart.dto.CartSummaryResponse;
import studio.aroundhub.tunagiftset.entity.Cart;
import studio.aroundhub.tunagiftset.entity.CartItem;
import studio.aroundhub.tunagiftset.entity.Member;
import studio.aroundhub.tunagiftset.entity.Product;
import studio.aroundhub.tunagiftset.entity.type.ProductStatus;
import studio.aroundhub.tunagiftset.exception.InvalidRequestException;
import studio.aroundhub.tunagiftset.exception.ResourceNotFoundException;
import studio.aroundhub.tunagiftset.repository.CartItemRepository;
import studio.aroundhub.tunagiftset.repository.CartRepository;
import studio.aroundhub.tunagiftset.repository.MemberRepository;
import studio.aroundhub.tunagiftset.repository.ProductImageRepository;
import studio.aroundhub.tunagiftset.repository.ProductRepository;
import studio.aroundhub.tunagiftset.storage.ObjectStorageService;

@Service
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final MemberRepository memberRepository;
    private final ShippingFeePolicy shippingFeePolicy;
    private final ObjectStorageService objectStorageService;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            ProductImageRepository productImageRepository,
            MemberRepository memberRepository,
            ShippingFeePolicy shippingFeePolicy,
            ObjectStorageService objectStorageService
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.memberRepository = memberRepository;
        this.shippingFeePolicy = shippingFeePolicy;
        this.objectStorageService = objectStorageService;
    }

    @Transactional
    public CartResponse addItem(Long memberId, CartItemAddRequest request) {
        Member member = getMember(memberId);
        Product product = getProduct(request.productId());
        validatePurchasable(product);

        Cart cart = getOrCreateCart(member);
        CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .orElse(null);

        int nextQuantity = request.quantity();
        if (cartItem != null) {
            nextQuantity += cartItem.getQuantity();
        }

        validateQuantity(nextQuantity, product);

        try {
            if (cartItem == null) {
                cartItemRepository.save(new CartItem(cart, product, nextQuantity));
            } else {
                cartItem.changeQuantity(nextQuantity);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new InvalidRequestException("CART_ITEM_CONFLICT", "장바구니 상품 처리 중 충돌이 발생했습니다. 다시 시도해 주세요.");
        }

        return getCart(memberId);
    }

    public CartResponse getCart(Long memberId) {
        Cart cart = cartRepository.findByMemberId(memberId).orElse(null);
        if (cart == null) {
            return new CartResponse(null, List.of(), new CartSummaryResponse(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        }

        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse updateQuantity(Long memberId, Long cartItemId, CartItemQuantityUpdateRequest request) {
        Cart cart = getOrCreateCart(getMember(memberId));
        CartItem cartItem = getCartItem(cartItemId, cart.getId());

        validatePurchasable(cartItem.getProduct());
        validateQuantity(request.quantity(), cartItem.getProduct());
        cartItem.changeQuantity(request.quantity());

        return buildCartResponse(cart);
    }

    @Transactional
    public void deleteItem(Long memberId, Long cartItemId) {
        Cart cart = getOrCreateCart(getMember(memberId));
        CartItem cartItem = getCartItem(cartItemId, cart.getId());
        cartItemRepository.delete(cartItem);
    }

    @Transactional
    public void deleteItems(Long memberId, CartItemDeleteRequest request) {
        if (request == null) {
            clear(memberId);
            return;
        }

        Cart cart = getOrCreateCart(getMember(memberId));
        List<Long> ids = request.cartItemIds();
        List<CartItem> cartItems = cartItemRepository.findByIdInAndCartId(ids, cart.getId());

        if (cartItems.size() != new HashSet<>(ids).size()) {
            throw new ResourceNotFoundException("CART_ITEM_NOT_FOUND", "장바구니 상품을 찾을 수 없습니다.");
        }

        cartItemRepository.deleteAll(cartItems);
    }

    @Transactional
    public void clear(Long memberId) {
        Cart cart = cartRepository.findByMemberId(memberId).orElse(null);
        if (cart != null) {
            cartItemRepository.deleteByCartId(cart.getId());
        }
    }

    private CartResponse buildCartResponse(Cart cart) {
        List<CartItem> cartItems = cartItemRepository.findAllWithProductByCartId(cart.getId());
        Map<Long, String> thumbnails = findThumbnails(cartItems);

        List<CartItemResponse> items = cartItems.stream()
                .map(cartItem -> CartItemResponse.from(cartItem, thumbnails.get(cartItem.getProduct().getId())))
                .toList();

        BigDecimal productAmount = items.stream()
                .filter(CartItemResponse::available)
                .map(CartItemResponse::itemAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalItemCount = items.stream()
                .filter(CartItemResponse::available)
                .mapToInt(CartItemResponse::quantity)
                .sum();
        BigDecimal shippingFee = shippingFeePolicy.calculate(productAmount);

        return new CartResponse(
                cart.getId(),
                items,
                new CartSummaryResponse(totalItemCount, productAmount, shippingFee, productAmount.add(shippingFee))
        );
    }

    private Map<Long, String> findThumbnails(List<CartItem> cartItems) {
        List<Long> productIds = cartItems.stream()
                .map(cartItem -> cartItem.getProduct().getId())
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

    private Cart getOrCreateCart(Member member) {
        return cartRepository.findByMemberId(member.getId())
                .orElseGet(() -> cartRepository.save(new Cart(member)));
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다."));
    }

    private Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다."));
    }

    private CartItem getCartItem(Long cartItemId, Long cartId) {
        return cartItemRepository.findByIdAndCartId(cartItemId, cartId)
                .orElseThrow(() -> new ResourceNotFoundException("CART_ITEM_NOT_FOUND", "장바구니 상품을 찾을 수 없습니다."));
    }

    private void validatePurchasable(Product product) {
        if (product.getStatus() == ProductStatus.SOLD_OUT || product.getStockQuantity() == 0) {
            throw new InvalidRequestException("PRODUCT_SOLD_OUT", "품절된 상품입니다.");
        }
        if (!product.isPurchasable()) {
            throw new InvalidRequestException("PRODUCT_NOT_AVAILABLE", "구매할 수 없는 상품입니다.");
        }
    }

    private void validateQuantity(int quantity, Product product) {
        if (quantity < 1 || quantity > 99) {
            throw new InvalidRequestException("INVALID_CART_QUANTITY", "장바구니 수량은 1개 이상 99개 이하여야 합니다.");
        }
        if (!product.hasEnoughStock(quantity)) {
            throw new InvalidRequestException("INSUFFICIENT_STOCK", "상품 재고가 부족합니다.");
        }
    }
}
