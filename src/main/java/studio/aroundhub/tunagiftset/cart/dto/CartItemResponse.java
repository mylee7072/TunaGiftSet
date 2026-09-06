package studio.aroundhub.tunagiftset.cart.dto;

import java.math.BigDecimal;
import studio.aroundhub.tunagiftset.entity.CartItem;
import studio.aroundhub.tunagiftset.entity.Product;
import studio.aroundhub.tunagiftset.entity.type.ProductStatus;

public record CartItemResponse(
        Long cartItemId,
        Long productId,
        String productCode,
        String productName,
        String thumbnailUrl,
        BigDecimal originalPrice,
        BigDecimal salePrice,
        int quantity,
        BigDecimal itemAmount,
        int stockQuantity,
        boolean available,
        String unavailableReason,
        boolean selected
) {
    public static CartItemResponse from(CartItem cartItem, String thumbnailUrl) {
        Product product = cartItem.getProduct();
        boolean available = product.isPurchasable() && product.hasEnoughStock(cartItem.getQuantity());

        return new CartItemResponse(
                cartItem.getId(),
                product.getId(),
                product.getProductCode(),
                product.getName(),
                thumbnailUrl,
                product.getOriginalPrice(),
                product.getSalePrice(),
                cartItem.getQuantity(),
                product.getSalePrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())),
                product.getStockQuantity(),
                available,
                available ? null : resolveUnavailableReason(product, cartItem.getQuantity()),
                available
        );
    }

    private static String resolveUnavailableReason(Product product, int quantity) {
        if (product.getStatus() == ProductStatus.SOLD_OUT || product.getStockQuantity() == 0) {
            return "SOLD_OUT";
        }
        if (product.getStatus() == ProductStatus.HIDDEN) {
            return "HIDDEN";
        }
        if (product.getStatus() == ProductStatus.DISCONTINUED) {
            return "DISCONTINUED";
        }
        if (!product.hasEnoughStock(quantity)) {
            return "INSUFFICIENT_STOCK";
        }
        return "PRODUCT_NOT_AVAILABLE";
    }
}
