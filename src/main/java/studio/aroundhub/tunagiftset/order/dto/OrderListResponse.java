package studio.aroundhub.tunagiftset.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import studio.aroundhub.tunagiftset.entity.Order;
import studio.aroundhub.tunagiftset.entity.OrderItem;
import studio.aroundhub.tunagiftset.entity.type.OrderStatus;

public record OrderListResponse(
        String orderNumber,
        OrderStatus orderStatus,
        String representativeProductName,
        int itemCount,
        BigDecimal totalAmount,
        Instant orderedAt
) {
    public static OrderListResponse from(Order order, List<OrderItem> items) {
        return new OrderListResponse(
                order.getOrderNumber(),
                order.getOrderStatus(),
                representativeProductName(items),
                items.stream().mapToInt(OrderItem::getQuantity).sum(),
                order.getTotalAmount(),
                order.getOrderedAt()
        );
    }

    private static String representativeProductName(List<OrderItem> items) {
        if (items.isEmpty()) {
            return "";
        }
        if (items.size() == 1) {
            return items.get(0).getProductName();
        }
        return items.get(0).getProductName() + " 외 " + (items.size() - 1) + "건";
    }
}
