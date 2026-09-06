package studio.aroundhub.tunagiftset.admin.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import studio.aroundhub.tunagiftset.entity.type.DeliveryStatus;
import studio.aroundhub.tunagiftset.entity.type.OrderStatus;
import studio.aroundhub.tunagiftset.entity.type.PaymentStatus;

public record AdminOrderListResponse(
        String orderNumber,
        String memberEmail,
        OrderStatus orderStatus,
        PaymentStatus paymentStatus,
        DeliveryStatus deliveryStatus,
        String representativeProductName,
        int itemCount,
        BigDecimal totalAmount,
        Instant orderedAt
) {
}
