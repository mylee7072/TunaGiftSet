package studio.aroundhub.tunagiftset.coupon.dto;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutPreviewResponse(
        BigDecimal productAmount,
        BigDecimal promotionDiscountAmount,
        BigDecimal couponDiscountAmount,
        BigDecimal shippingFee,
        BigDecimal totalAmount,
        Long selectedMemberCouponId,
        List<AvailableCouponResponse> coupons
) {
}
