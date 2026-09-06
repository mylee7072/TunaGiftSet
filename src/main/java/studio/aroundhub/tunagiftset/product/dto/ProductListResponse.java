package studio.aroundhub.tunagiftset.product.dto;

import java.math.BigDecimal;
import studio.aroundhub.tunagiftset.entity.Product;
import studio.aroundhub.tunagiftset.entity.type.ProductStatus;

public record ProductListResponse(
        Long id,
        String productCode,
        String name,
        Long brandId,
        String brandDisplayName,
        Long categoryId,
        String categoryName,
        String thumbnailImageUrl,
        BigDecimal originalPrice,
        BigDecimal salePrice,
        int stockQuantity,
        ProductStatus status,
        boolean featured,
        BigDecimal averageRating,
        long reviewCount,
        long wishlistCount
) {
    public static ProductListResponse from(Product product, String thumbnailImageUrl) {
        return from(product, thumbnailImageUrl, BigDecimal.ZERO, 0, 0);
    }

    public static ProductListResponse from(Product product, String thumbnailImageUrl, BigDecimal averageRating, long reviewCount, long wishlistCount) {
        return new ProductListResponse(
                product.getId(),
                product.getProductCode(),
                product.getName(),
                product.getBrand().getId(),
                product.getBrand().getDisplayName(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                thumbnailImageUrl,
                product.getOriginalPrice(),
                product.getSalePrice(),
                product.getStockQuantity(),
                product.getStatus(),
                product.isFeatured(),
                averageRating,
                reviewCount,
                wishlistCount
        );
    }
}
