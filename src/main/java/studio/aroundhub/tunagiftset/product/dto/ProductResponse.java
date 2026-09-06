package studio.aroundhub.tunagiftset.product.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import studio.aroundhub.tunagiftset.entity.Product;
import studio.aroundhub.tunagiftset.entity.type.ProductStatus;

public record ProductResponse(
        Long id,
        String productCode,
        String name,
        Long brandId,
        String brandName,
        String brandDisplayName,
        Long categoryId,
        String categoryName,
        String shortDescription,
        String description,
        BigDecimal originalPrice,
        BigDecimal salePrice,
        int discountRate,
        int stockQuantity,
        boolean soldOut,
        ProductStatus status,
        boolean featured,
        BigDecimal averageRating,
        long reviewCount,
        long wishlistCount,
        List<ProductImageResponse> images
) {
    public static ProductResponse from(Product product, List<ProductImageResponse> images) {
        return from(product, images, BigDecimal.ZERO, 0, 0);
    }

    public static ProductResponse from(Product product, List<ProductImageResponse> images, BigDecimal averageRating, long reviewCount, long wishlistCount) {
        return new ProductResponse(
                product.getId(),
                product.getProductCode(),
                product.getName(),
                product.getBrand().getId(),
                product.getBrand().getName(),
                product.getBrand().getDisplayName(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getShortDescription(),
                product.getDescription(),
                product.getOriginalPrice(),
                product.getSalePrice(),
                calculateDiscountRate(product.getOriginalPrice(), product.getSalePrice()),
                product.getStockQuantity(),
                product.getStockQuantity() == 0 || product.getStatus() == ProductStatus.SOLD_OUT,
                product.getStatus(),
                product.isFeatured(),
                averageRating,
                reviewCount,
                wishlistCount,
                images
        );
    }

    private static int calculateDiscountRate(BigDecimal originalPrice, BigDecimal salePrice) {
        if (originalPrice == null || salePrice == null || originalPrice.signum() <= 0) {
            return 0;
        }

        BigDecimal discountAmount = originalPrice.subtract(salePrice);
        if (discountAmount.signum() <= 0) {
            return 0;
        }

        return discountAmount
                .multiply(BigDecimal.valueOf(100))
                .divide(originalPrice, 0, RoundingMode.DOWN)
                .intValue();
    }
}
