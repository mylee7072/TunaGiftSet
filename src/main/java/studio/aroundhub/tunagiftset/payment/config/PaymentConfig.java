package studio.aroundhub.tunagiftset.payment.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
