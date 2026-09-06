package studio.aroundhub.tunagiftset.payment.client;

import java.math.BigDecimal;

public record TossPaymentCancelCommand(
        String paymentKey,
        String cancelReason,
        BigDecimal cancelAmount,
        String idempotencyKey
) {
}
