package studio.aroundhub.tunagiftset.order.dto;

import java.math.BigDecimal;
import studio.aroundhub.tunagiftset.entity.OrderItem;

public record OrderItemResponse(
        Long orderItemId,
        Long productId,
        String productCode,
        String productName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal totalPrice
) {
    public static OrderItemResponse from(OrderItem orderItem) {
        Long productId = orderItem.getProduct() == null ? null : orderItem.getProduct().getId();
        return new OrderItemResponse(
                orderItem.getId(),
                productId,
                orderItem.getProductCode(),
                orderItem.getProductName(),
                orderItem.getUnitPrice(),
                orderItem.getQuantity(),
                orderItem.getTotalPrice()
        );
    }
}
