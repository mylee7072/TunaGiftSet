package studio.aroundhub.tunagiftset.product.service;

import jakarta.persistence.criteria.JoinType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studio.aroundhub.tunagiftset.brand.service.BrandService;
import studio.aroundhub.tunagiftset.category.service.CategoryService;
import studio.aroundhub.tunagiftset.entity.Brand;
import studio.aroundhub.tunagiftset.entity.Category;
import studio.aroundhub.tunagiftset.entity.Product;
import studio.aroundhub.tunagiftset.entity.ProductImage;
import studio.aroundhub.tunagiftset.entity.type.ProductStatus;
import studio.aroundhub.tunagiftset.exception.DuplicateResourceException;
import studio.aroundhub.tunagiftset.exception.ResourceNotFoundException;
import studio.aroundhub.tunagiftset.product.dto.ProductCreateRequest;
import studio.aroundhub.tunagiftset.product.dto.ProductImageResponse;
import studio.aroundhub.tunagiftset.product.dto.ProductListResponse;
import studio.aroundhub.tunagiftset.product.dto.ProductPageResponse;
import studio.aroundhub.tunagiftset.product.dto.ProductResponse;
import studio.aroundhub.tunagiftset.product.dto.ProductSortType;
import studio.aroundhub.tunagiftset.product.dto.ProductStatusUpdateRequest;
import studio.aroundhub.tunagiftset.product.dto.ProductUpdateRequest;
import studio.aroundhub.tunagiftset.product.dto.ReviewSummaryResponse;
import studio.aroundhub.tunagiftset.repository.ProductImageRepository;
import studio.aroundhub.tunagiftset.repository.ProductRepository;
import studio.aroundhub.tunagiftset.storage.ObjectStorageService;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final BrandService brandService;
    private final CategoryService categoryService;
    private final ObjectStorageService objectStorageService;
    private final ReviewService reviewService;
    private final WishlistService wishlistService;

    public ProductService(
            ProductRepository productRepository,
            ProductImageRepository productImageRepository,
            BrandService brandService,
            CategoryService categoryService,
            ObjectStorageService objectStorageService,
            ReviewService reviewService,
            WishlistService wishlistService
    ) {
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.brandService = brandService;
        this.categoryService = categoryService;
        this.objectStorageService = objectStorageService;
        this.reviewService = reviewService;
        this.wishlistService = wishlistService;
    }

    @Transactional
    public ProductResponse create(ProductCreateRequest request) {
        Brand brand = brandService.getBrand(request.brandId());
        Category category = categoryService.getCategory(request.categoryId());

        if (productRepository.existsByProductCode(request.productCode())) {
            throw new DuplicateResourceException("PRODUCT_CODE_DUPLICATED", "이미 사용 중인 상품 코드입니다.");
        }

        Product product = new Product(
                brand,
                category,
                request.productCode(),
                request.name(),
                request.shortDescription(),
                request.description(),
                request.originalPrice(),
                request.salePrice(),
                request.stockQuantity(),
                request.status(),
                request.featured()
        );

        return ProductResponse.from(productRepository.save(product), List.of());
    }

    @Transactional
    public ProductResponse update(Long productId, ProductUpdateRequest request) {
        Product product = getProduct(productId);
        Brand brand = brandService.getBrand(request.brandId());
        Category category = categoryService.getCategory(request.categoryId());

        if (productRepository.existsByProductCodeAndIdNot(request.productCode(), productId)) {
            throw new DuplicateResourceException("PRODUCT_CODE_DUPLICATED", "이미 사용 중인 상품 코드입니다.");
        }

        product.update(
                brand,
                category,
                request.productCode(),
                request.name(),
                request.shortDescription(),
                request.description(),
                request.originalPrice(),
                request.salePrice(),
                request.stockQuantity(),
                request.status(),
                request.featured()
        );

        return toProductResponse(product);
    }

    @Transactional
    public ProductResponse changeStatus(Long productId, ProductStatusUpdateRequest request) {
        Product product = getProduct(productId);
        product.changeStatus(request.status());

        return toProductResponse(product);
    }

    public ProductPageResponse findProducts(
            String keyword,
            Long categoryId,
            Long brandId,
            ProductStatus status,
            Boolean featured,
            ProductSortType sort,
            int page,
            int size
    ) {
        Specification<Product> specification = userVisible()
                .and(nameContains(keyword))
                .and(categoryEquals(categoryId))
                .and(brandEquals(brandId))
                .and(statusEquals(status))
                .and(featuredEquals(featured));

        return searchProducts(specification, sort, page, size);
    }

    public ProductResponse findProduct(Long productId) {
        Product product = getProduct(productId);
        if (product.getStatus() == ProductStatus.HIDDEN || product.getStatus() == ProductStatus.DISCONTINUED) {
            throw new ResourceNotFoundException("PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다.");
        }

        return toProductResponse(product);
    }

    /** Admin management listing — every status is visible (HIDDEN/DISCONTINUED included), unlike the storefront listing above. */
    public ProductPageResponse findAdminProducts(
            String keyword,
            Long categoryId,
            Long brandId,
            ProductStatus status,
            Boolean featured,
            ProductSortType sort,
            int page,
            int size
    ) {
        Specification<Product> specification = nameContains(keyword)
                .and(categoryEquals(categoryId))
                .and(brandEquals(brandId))
                .and(statusEquals(status))
                .and(featuredEquals(featured));

        return searchProducts(specification, sort, page, size);
    }

    /** Admin single-product lookup — unlike {@link #findProduct}, does not hide HIDDEN/DISCONTINUED products (an admin must be able to re-edit them). */
    public ProductResponse findAdminProduct(Long productId) {
        return toProductResponse(getProduct(productId));
    }

    private ProductPageResponse searchProducts(Specification<Product> specification, ProductSortType sort, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                toSort(sort)
        );

        Page<Product> products = productRepository.findAll(specification, pageable);
        Map<Long, String> thumbnails = findThumbnails(products.getContent());
        Map<Long, ReviewSummaryResponse> reviewSummaries = reviewService.summarize(products.getContent().stream().map(Product::getId).toList());
        Map<Long, Long> wishlistCounts = wishlistService.countByProductIds(products.getContent().stream().map(Product::getId).toList());
        Page<ProductListResponse> result = products
                .map(product -> {
                    ReviewSummaryResponse summary = reviewSummaries.getOrDefault(product.getId(), ReviewSummaryResponse.empty());
                    return ProductListResponse.from(
                            product,
                            thumbnails.get(product.getId()),
                            summary.averageRating(),
                            summary.reviewCount(),
                            wishlistCounts.getOrDefault(product.getId(), 0L)
                    );
                });

        return ProductPageResponse.from(result);
    }

    private Map<Long, String> findThumbnails(List<Product> products) {
        List<Long> productIds = products.stream().map(Product::getId).toList();
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

    private ProductResponse toProductResponse(Product product) {
        List<ProductImageResponse> images = productImageRepository
                .findByProductIdOrderByDisplayOrderAsc(product.getId())
                .stream()
                .map(image -> ProductImageResponse.from(image, resolveImageUrl(image.getObjectKey(), image.getImageUrl())))
                .toList();

        ReviewSummaryResponse summary = reviewService.summarize(product.getId());
        long wishlistCount = wishlistService.countByProductIds(List.of(product.getId())).getOrDefault(product.getId(), 0L);
        return ProductResponse.from(product, images, summary.averageRating(), summary.reviewCount(), wishlistCount);
    }

    private String resolveImageUrl(String objectKey, String fallbackUrl) {
        if (objectKey == null || objectKey.isBlank()) {
            return fallbackUrl;
        }
        return objectStorageService.publicUrl(objectKey);
    }

    private Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다."));
    }

    private Sort toSort(ProductSortType sort) {
        ProductSortType sortType = sort == null ? ProductSortType.LATEST : sort;

        return switch (sortType) {
            case PRICE_ASC -> Sort.by(Sort.Direction.ASC, "salePrice").and(Sort.by(Sort.Direction.DESC, "id"));
            case PRICE_DESC -> Sort.by(Sort.Direction.DESC, "salePrice").and(Sort.by(Sort.Direction.DESC, "id"));
            case LATEST -> Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
        };
    }

    private Specification<Product> userVisible() {
        return (root, query, criteriaBuilder) ->
                root.get("status").in(ProductStatus.ACTIVE, ProductStatus.SOLD_OUT);
    }

    private Specification<Product> nameContains(String keyword) {
        return (root, query, criteriaBuilder) -> {
            fetchBrandAndCategory(root, query);

            if (keyword == null || keyword.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")),
                    "%" + keyword.trim().toLowerCase() + "%"
            );
        };
    }

    private Specification<Product> categoryEquals(Long categoryId) {
        return (root, query, criteriaBuilder) ->
                categoryId == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("category").get("id"), categoryId);
    }

    private Specification<Product> brandEquals(Long brandId) {
        return (root, query, criteriaBuilder) ->
                brandId == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("brand").get("id"), brandId);
    }

    private Specification<Product> statusEquals(ProductStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("status"), status);
    }

    private Specification<Product> featuredEquals(Boolean featured) {
        return (root, query, criteriaBuilder) ->
                featured == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("featured"), featured);
    }

    private void fetchBrandAndCategory(jakarta.persistence.criteria.Root<Product> root, jakarta.persistence.criteria.CriteriaQuery<?> query) {
        if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
            root.fetch("brand", JoinType.LEFT);
            root.fetch("category", JoinType.LEFT);
            query.distinct(true);
        }
    }
}
