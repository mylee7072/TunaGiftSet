package studio.aroundhub.tunagiftset.product.dto;

import java.math.BigDecimal;
import java.time.Instant;
import studio.aroundhub.tunagiftset.entity.Product;
import studio.aroundhub.tunagiftset.entity.Wishlist;
import studio.aroundhub.tunagiftset.entity.type.ProductStatus;

public record WishlistItemResponse(
        Long wishlistId,
        Long productId,
        String productName,
        String brandDisplayName,
        String categoryName,
        String thumbnailImageUrl,
        BigDecimal originalPrice,
        BigDecimal salePrice,
        int stockQuantity,
        ProductStatus status,
        Instant createdAt
) {
    public static WishlistItemResponse from(Wishlist wishlist, String thumbnailImageUrl) {
        Product product = wishlist.getProduct();
        return new WishlistItemResponse(
                wishlist.getId(),
                product.getId(),
                product.getName(),
                product.getBrand().getDisplayName(),
                product.getCategory().getName(),
                thumbnailImageUrl,
                product.getOriginalPrice(),
                product.getSalePrice(),
                product.getStockQuantity(),
                product.getStatus(),
                wishlist.getCreatedAt()
        );
    }
}
