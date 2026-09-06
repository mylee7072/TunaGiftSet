package studio.aroundhub.tunagiftset.coupon.service;

import java.math.BigDecimal;
import studio.aroundhub.tunagiftset.entity.MemberCoupon;

public record DiscountCalculation(
        BigDecimal productAmount,
        BigDecimal promotionDiscountAmount,
        BigDecimal couponDiscountAmount,
        BigDecimal shippingFee,
        BigDecimal totalAmount,
        MemberCoupon memberCoupon
) {
}
