package studio.aroundhub.tunagiftset.coupon.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import studio.aroundhub.tunagiftset.entity.type.CouponStatus;
import studio.aroundhub.tunagiftset.entity.type.DiscountType;

public record CouponCreateRequest(
        @NotBlank @Size(max = 150)
        String name,

        @NotBlank @Size(max = 50)
        String code,

        @NotNull
        DiscountType discountType,

        @NotNull @DecimalMin(value = "1")
        BigDecimal discountValue,

        @NotNull @DecimalMin(value = "0")
        BigDecimal minimumOrderAmount,

        @DecimalMin(value = "1")
        BigDecimal maximumDiscountAmount,

        @NotNull
        Instant validFrom,

        @NotNull
        Instant validUntil,

        CouponStatus status,

        @Min(1)
        Integer totalIssueLimit,

        @Min(1)
        Integer perMemberLimit
) {
}
