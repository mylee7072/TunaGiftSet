package studio.aroundhub.tunagiftset.coupon.dto;

import java.math.BigDecimal;
import java.time.Instant;
import studio.aroundhub.tunagiftset.entity.Coupon;
import studio.aroundhub.tunagiftset.entity.type.CouponStatus;
import studio.aroundhub.tunagiftset.entity.type.DiscountType;

public record CouponResponse(
        Long id,
        String name,
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal minimumOrderAmount,
        BigDecimal maximumDiscountAmount,
        Instant validFrom,
        Instant validUntil,
        CouponStatus status,
        Integer totalIssueLimit,
        int perMemberLimit
) {
    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getName(),
                coupon.getCode(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMinimumOrderAmount(),
                coupon.getMaximumDiscountAmount(),
                coupon.getValidFrom(),
                coupon.getValidUntil(),
                coupon.getStatus(),
                coupon.getTotalIssueLimit(),
                coupon.getPerMemberLimit()
        );
    }
}
