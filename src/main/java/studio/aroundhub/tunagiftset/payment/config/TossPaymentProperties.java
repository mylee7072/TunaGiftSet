package studio.aroundhub.tunagiftset.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.toss")
public record TossPaymentProperties(
        String baseUrl,
        String secretKey,
        int connectTimeoutMillis,
        int readTimeoutMillis
) {
    public String baseUrl() {
        return baseUrl == null || baseUrl.isBlank() ? "https://api.tosspayments.com" : baseUrl;
    }

    public int connectTimeoutMillis() {
        return connectTimeoutMillis <= 0 ? 3000 : connectTimeoutMillis;
    }

    public int readTimeoutMillis() {
        return readTimeoutMillis <= 0 ? 5000 : readTimeoutMillis;
    }
}
