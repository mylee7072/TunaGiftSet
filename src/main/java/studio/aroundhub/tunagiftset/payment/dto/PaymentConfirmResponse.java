package studio.aroundhub.tunagiftset.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import studio.aroundhub.tunagiftset.entity.Order;
import studio.aroundhub.tunagiftset.entity.Payment;
import studio.aroundhub.tunagiftset.entity.type.OrderStatus;
import studio.aroundhub.tunagiftset.entity.type.PaymentMethod;
import studio.aroundhub.tunagiftset.entity.type.PaymentStatus;

public record PaymentConfirmResponse(
        String orderNumber,
        OrderStatus orderStatus,
        PaymentStatus paymentStatus,
        BigDecimal amount,
        PaymentMethod method,
        Instant approvedAt
) {
    public static PaymentConfirmResponse from(Order order, Payment payment) {
        return new PaymentConfirmResponse(
                order.getOrderNumber(),
                order.getOrderStatus(),
                payment.getStatus(),
                payment.getApprovedAmount(),
                payment.getPaymentMethod(),
                payment.getPaidAt()
        );
    }
}
