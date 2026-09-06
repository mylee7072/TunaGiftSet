package studio.aroundhub.tunagiftset.cart.dto;

import java.math.BigDecimal;

public record CartSummaryResponse(
        int totalItemCount,
        BigDecimal totalProductAmount,
        BigDecimal shippingFee,
        BigDecimal totalAmount
) {
}
