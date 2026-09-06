package studio.aroundhub.tunagiftset.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record PaymentConfirmRequest(
        @NotBlank(message = "paymentKey is required.")
        String paymentKey,

        @NotBlank(message = "orderId is required.")
        String orderId,

        @NotNull(message = "amount is required.")
        @Positive(message = "amount must be positive.")
        BigDecimal amount
) {
}
