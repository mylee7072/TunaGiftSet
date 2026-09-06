package studio.aroundhub.tunagiftset.coupon.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CheckoutPreviewRequest(
        @NotEmpty
        List<Long> cartItemIds,

        Long memberCouponId
) {
}
