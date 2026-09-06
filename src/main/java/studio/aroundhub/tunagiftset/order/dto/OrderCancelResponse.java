package studio.aroundhub.tunagiftset.order.dto;

import studio.aroundhub.tunagiftset.entity.Order;
import studio.aroundhub.tunagiftset.entity.type.OrderStatus;

public record OrderCancelResponse(
        String orderNumber,
        OrderStatus orderStatus
) {
    public static OrderCancelResponse from(Order order) {
        return new OrderCancelResponse(order.getOrderNumber(), order.getOrderStatus());
    }
}
