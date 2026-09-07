package studio.aroundhub.tunagiftset.auth.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oauth.kakao")
public record KakaoOAuthProperties(String clientId, String clientSecret) {
}
