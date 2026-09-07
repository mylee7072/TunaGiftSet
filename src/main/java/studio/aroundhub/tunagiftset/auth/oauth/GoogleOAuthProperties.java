package studio.aroundhub.tunagiftset.auth.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oauth.google")
public record GoogleOAuthProperties(String clientId, String clientSecret) {
}
