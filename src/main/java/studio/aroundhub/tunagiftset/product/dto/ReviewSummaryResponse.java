package studio.aroundhub.tunagiftset.product.dto;

import java.math.BigDecimal;

public record ReviewSummaryResponse(
        BigDecimal averageRating,
        long reviewCount
) {
    public static ReviewSummaryResponse empty() {
        return new ReviewSummaryResponse(BigDecimal.ZERO, 0);
    }
}
