package studio.aroundhub.tunagiftset.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import studio.aroundhub.tunagiftset.entity.Payment;
import studio.aroundhub.tunagiftset.entity.type.PaymentMethod;
import studio.aroundhub.tunagiftset.entity.type.PaymentStatus;

public record OrderPaymentResponse(
        PaymentStatus status,
        PaymentMethod paymentMethod,
        BigDecimal amount,
        BigDecimal approvedAmount,
        Instant paidAt,
        Instant canceledAt
) {
    public static OrderPaymentResponse from(Payment payment) {
        if (payment == null) {
            return null;
        }
        return new OrderPaymentResponse(
                payment.getStatus(),
                payment.getPaymentMethod(),
                payment.getAmount(),
                payment.getApprovedAmount(),
                payment.getPaidAt(),
                payment.getCanceledAt()
        );
    }
}
