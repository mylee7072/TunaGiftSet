package studio.aroundhub.tunagiftset.coupon.dto;

import java.math.BigDecimal;
import java.time.Instant;
import studio.aroundhub.tunagiftset.entity.MemberCoupon;
import studio.aroundhub.tunagiftset.entity.type.DiscountType;
import studio.aroundhub.tunagiftset.entity.type.MemberCouponStatus;

public record MemberCouponResponse(
        Long memberCouponId,
        Long couponId,
        String name,
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal minimumOrderAmount,
        BigDecimal maximumDiscountAmount,
        Instant validFrom,
        Instant validUntil,
        MemberCouponStatus status,
        boolean usableNow,
        Instant issuedAt,
        Instant reservedAt,
        Instant usedAt
) {
    public static MemberCouponResponse from(MemberCoupon memberCoupon, boolean usableNow) {
        var coupon = memberCoupon.getCoupon();
        return new MemberCouponResponse(
                memberCoupon.getId(),
                coupon.getId(),
                coupon.getName(),
                coupon.getCode(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMinimumOrderAmount(),
                coupon.getMaximumDiscountAmount(),
                coupon.getValidFrom(),
                coupon.getValidUntil(),
                memberCoupon.getStatus(),
                usableNow,
                memberCoupon.getIssuedAt(),
                memberCoupon.getReservedAt(),
                memberCoupon.getUsedAt()
        );
    }
}
