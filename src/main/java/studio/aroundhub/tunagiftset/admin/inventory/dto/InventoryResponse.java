package studio.aroundhub.tunagiftset.admin.inventory.dto;

import studio.aroundhub.tunagiftset.entity.Product;
import studio.aroundhub.tunagiftset.entity.type.ProductStatus;

public record InventoryResponse(
        Long productId,
        String productCode,
        String productName,
        int stockQuantity,
        ProductStatus status
) {
    public static InventoryResponse from(Product product) {
        return new InventoryResponse(product.getId(), product.getProductCode(), product.getName(), product.getStockQuantity(), product.getStatus());
    }
}
