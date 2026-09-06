package studio.aroundhub.tunagiftset.cart.service;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class ShippingFeePolicy {

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = BigDecimal.valueOf(50000);
    private static final BigDecimal BASIC_SHIPPING_FEE = BigDecimal.valueOf(3000);

    public BigDecimal calculate(BigDecimal productAmount) {
        if (productAmount.signum() == 0 || productAmount.compareTo(FREE_SHIPPING_THRESHOLD) >= 0) {
            return BigDecimal.ZERO;
        }
        return BASIC_SHIPPING_FEE;
    }
}
