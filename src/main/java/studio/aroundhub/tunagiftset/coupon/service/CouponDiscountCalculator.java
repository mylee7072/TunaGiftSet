package studio.aroundhub.tunagiftset.coupon.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;
import studio.aroundhub.tunagiftset.entity.Coupon;
import studio.aroundhub.tunagiftset.entity.type.DiscountType;

@Component
public class CouponDiscountCalculator {

    public BigDecimal calculate(Coupon coupon, BigDecimal productAmount) {
        if (productAmount == null || productAmount.signum() <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = switch (coupon.getDiscountType()) {
            case FIXED_AMOUNT -> coupon.getDiscountValue();
            case PERCENTAGE -> productAmount
                    .multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
        };

        if (coupon.getDiscountType() == DiscountType.PERCENTAGE
                && coupon.getMaximumDiscountAmount() != null
                && discount.compareTo(coupon.getMaximumDiscountAmount()) > 0) {
            discount = coupon.getMaximumDiscountAmount();
        }

        if (discount.compareTo(productAmount) > 0) {
            return productAmount;
        }
        return discount.max(BigDecimal.ZERO).setScale(0, RoundingMode.DOWN);
    }
}
