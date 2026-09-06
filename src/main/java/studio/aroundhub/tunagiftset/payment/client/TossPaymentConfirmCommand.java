package studio.aroundhub.tunagiftset.payment.client;

import java.math.BigDecimal;

public record TossPaymentConfirmCommand(
        String paymentKey,
        String orderId,
        BigDecimal amount,
        String idempotencyKey
) {
}
