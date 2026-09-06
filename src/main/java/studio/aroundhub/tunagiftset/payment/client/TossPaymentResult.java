package studio.aroundhub.tunagiftset.payment.client;

import java.math.BigDecimal;
import java.time.Instant;
import studio.aroundhub.tunagiftset.entity.type.PaymentMethod;

public record TossPaymentResult(
        String paymentKey,
        String orderId,
        BigDecimal totalAmount,
        String currency,
        PaymentMethod method,
        String providerStatus,
        Instant approvedAt,
        Instant canceledAt
) {
}
