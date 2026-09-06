package studio.aroundhub.tunagiftset.coupon.service;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class PromotionDiscountService {

    public BigDecimal calculateOrderDiscount(BigDecimal productAmount) {
        return BigDecimal.ZERO;
    }
}
