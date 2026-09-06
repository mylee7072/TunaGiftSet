package studio.aroundhub.tunagiftset.coupon.dto;

import java.math.BigDecimal;
import java.time.Instant;
import studio.aroundhub.tunagiftset.entity.MemberCoupon;
import studio.aroundhub.tunagiftset.entity.type.DiscountType;

public record AvailableCouponResponse(
        Long memberCouponId,
        String name,
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal minimumOrderAmount,
        BigDecimal maximumDiscountAmount,
        Instant validFrom,
        Instant validUntil,
        boolean usable,
        String unavailableReason,
        BigDecimal expectedDiscountAmount
) {
    public static AvailableCouponResponse of(
            MemberCoupon memberCoupon,
            boolean usable,
            String unavailableReason,
            BigDecimal expectedDiscountAmount
    ) {
        var coupon = memberCoupon.getCoupon();
        return new AvailableCouponResponse(
                memberCoupon.getId(),
                coupon.getName(),
                coupon.getCode(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMinimumOrderAmount(),
                coupon.getMaximumDiscountAmount(),
                coupon.getValidFrom(),
                coupon.getValidUntil(),
                usable,
                unavailableReason,
                expectedDiscountAmount
        );
    }
}
