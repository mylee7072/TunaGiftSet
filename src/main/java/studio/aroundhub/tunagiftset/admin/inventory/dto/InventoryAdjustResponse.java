package studio.aroundhub.tunagiftset.admin.inventory.dto;

import studio.aroundhub.tunagiftset.entity.Product;

public record InventoryAdjustResponse(
        Long productId,
        int quantityBefore,
        int changeQuantity,
        int quantityAfter,
        String productStatus
) {
    public static InventoryAdjustResponse of(Product product, int quantityBefore, int changeQuantity) {
        return new InventoryAdjustResponse(
                product.getId(),
                quantityBefore,
                changeQuantity,
                product.getStockQuantity(),
                product.getStatus().name()
        );
    }
}
