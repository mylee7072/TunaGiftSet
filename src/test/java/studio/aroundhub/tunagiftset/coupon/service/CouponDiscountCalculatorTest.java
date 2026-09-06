package studio.aroundhub.tunagiftset.coupon.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import studio.aroundhub.tunagiftset.entity.Coupon;
import studio.aroundhub.tunagiftset.entity.type.CouponStatus;
import studio.aroundhub.tunagiftset.entity.type.DiscountType;

class CouponDiscountCalculatorTest {

    private final CouponDiscountCalculator calculator = new CouponDiscountCalculator();

    @Test
    void fixedAmountCouponDiscountsExactValue() {
        Coupon coupon = fixedAmountCoupon(BigDecimal.valueOf(5000), null);

        BigDecimal discount = calculator.calculate(coupon, BigDecimal.valueOf(50000));

        assertThat(discount).isEqualByComparingTo(BigDecimal.valueOf(5000));
    }

    @Test
    void fixedAmountCouponNeverExceedsProductAmount() {
        Coupon coupon = fixedAmountCoupon(BigDecimal.valueOf(5000), null);

        BigDecimal discount = calculator.calculate(coupon, BigDecimal.valueOf(3000));

        assertThat(discount).isEqualByComparingTo(BigDecimal.valueOf(3000));
    }

    @Test
    void percentageCouponCalculatesExpectedDiscount() {
        Coupon coupon = percentageCoupon(BigDecimal.valueOf(10), null);

        BigDecimal discount = calculator.calculate(coupon, BigDecimal.valueOf(50000));

        assertThat(discount).isEqualByComparingTo(BigDecimal.valueOf(5000));
    }

    @Test
    void percentageCouponBelowMaximumDiscountIsUnaffected() {
        Coupon coupon = percentageCoupon(BigDecimal.valueOf(10), BigDecimal.valueOf(10000));

        BigDecimal discount = calculator.calculate(coupon, BigDecimal.valueOf(50000));

        assertThat(discount).isEqualByComparingTo(BigDecimal.valueOf(5000));
    }

    @Test
    void percentageCouponIsCappedByMaximumDiscountAmount() {
        Coupon coupon = percentageCoupon(BigDecimal.valueOf(10), BigDecimal.valueOf(10000));

        BigDecimal discount = calculator.calculate(coupon, BigDecimal.valueOf(200000));

        assertThat(discount).isEqualByComparingTo(BigDecimal.valueOf(10000));
    }

    @Test
    void percentageRoundingTruncatesFractionalWon() {
        // 33333 * 10% = 3333.3 -> truncated down to 3333, not rounded to 3333 vs 3334.
        Coupon coupon = percentageCoupon(BigDecimal.valueOf(10), null);

        BigDecimal discount = calculator.calculate(coupon, BigDecimal.valueOf(33333));

        assertThat(discount).isEqualByComparingTo(BigDecimal.valueOf(3333));
    }

    @Test
    void onePercentCouponCalculatesCorrectly() {
        Coupon coupon = percentageCoupon(BigDecimal.valueOf(1), null);

        BigDecimal discount = calculator.calculate(coupon, BigDecimal.valueOf(10000));

        assertThat(discount).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void oneHundredPercentCouponDiscountsFullAmount() {
        Coupon coupon = percentageCoupon(BigDecimal.valueOf(100), null);

        BigDecimal discount = calculator.calculate(coupon, BigDecimal.valueOf(10000));

        assertThat(discount).isEqualByComparingTo(BigDecimal.valueOf(10000));
    }

    @Test
    void zeroOrNegativeProductAmountYieldsZeroDiscount() {
        Coupon coupon = fixedAmountCoupon(BigDecimal.valueOf(5000), null);

        assertThat(calculator.calculate(coupon, BigDecimal.ZERO)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(calculator.calculate(coupon, BigDecimal.valueOf(-1000))).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(calculator.calculate(coupon, null)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private Coupon fixedAmountCoupon(BigDecimal discountValue, BigDecimal maximumDiscountAmount) {
        return coupon(DiscountType.FIXED_AMOUNT, discountValue, maximumDiscountAmount);
    }

    private Coupon percentageCoupon(BigDecimal discountValue, BigDecimal maximumDiscountAmount) {
        return coupon(DiscountType.PERCENTAGE, discountValue, maximumDiscountAmount);
    }

    private Coupon coupon(DiscountType discountType, BigDecimal discountValue, BigDecimal maximumDiscountAmount) {
        Instant now = Instant.now();
        return new Coupon(
                "SAMPLE COUPON",
                "SAMPLE" + discountType + discountValue,
                discountType,
                discountValue,
                BigDecimal.ZERO,
                maximumDiscountAmount,
                now.minus(1, ChronoUnit.DAYS),
                now.plus(1, ChronoUnit.DAYS),
                CouponStatus.ACTIVE,
                null,
                1
        );
    }
}
