package studio.aroundhub.tunagiftset.auth.oauth;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import studio.aroundhub.tunagiftset.entity.type.MemberProvider;
import studio.aroundhub.tunagiftset.exception.AuthenticationFailedException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Talks to Kakao's OAuth endpoints (developers.kakao.com/docs/latest/ko/kakaologin). Kakao's
 * "카카오계정(이메일)" consent item requires the Kakao app to pass business-app review —
 * until that's granted, kakao_account.email is simply absent from the response. Rather than
 * block login on that (review can take a while and isn't guaranteed), we fall back to a
 * synthetic per-account email so signup/login still works; that member just can't receive
 * account emails (order confirmations etc.) until the app is verified and they re-link.
 */
@Component
public class KakaoOAuthClient implements OAuthClient {

    private static final Logger log = LoggerFactory.getLogger(KakaoOAuthClient.class);
    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KakaoOAuthProperties properties;

    public KakaoOAuthClient(KakaoOAuthProperties properties) {
        this.properties = properties;
    }

    @Override
    public MemberProvider providerType() {
        return MemberProvider.KAKAO;
    }

    @Override
    public OAuthUserInfo authenticate(String code, String redirectUri) {
        requireClientId();
        String accessToken = exchangeToken(code, redirectUri);
        return fetchUserInfo(accessToken);
    }

    private String exchangeToken(String code, String redirectUri) {
        StringBuilder form = new StringBuilder()
                .append("grant_type=authorization_code")
                .append("&client_id=").append(encode(properties.clientId()))
                .append("&redirect_uri=").append(encode(redirectUri))
                .append("&code=").append(encode(code));
        if (properties.clientSecret() != null && !properties.clientSecret().isBlank()) {
            form.append("&client_secret=").append(encode(properties.clientSecret()));
        }

        JsonNode root = parse(post(TOKEN_URL, form.toString(), "token exchange"), "token exchange");
        String accessToken = root.path("access_token").asString(null);
        if (accessToken == null || accessToken.isBlank()) {
            throw loginFailed("token exchange returned no access_token");
        }
        return accessToken;
    }

    private OAuthUserInfo fetchUserInfo(String accessToken) {
        String responseBody;
        try {
            responseBody = restClient.get()
                    .uri(USER_INFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException | ResourceAccessException exception) {
            log.warn("Kakao user info request failed.", exception);
            throw loginFailed("user info request failed");
        }

        JsonNode root = parse(responseBody, "user info");
        String providerId = root.path("id").asString(null);
        JsonNode kakaoAccount = root.path("kakao_account");
        String email = kakaoAccount.path("email").asString(null);
        String name = kakaoAccount.path("profile").path("nickname").asString(null);

        if (providerId == null) {
            throw loginFailed("user info missing id");
        }
        if (email == null || email.isBlank()) {
            email = "kakao_" + providerId + "@kakao.local";
        }
        return new OAuthUserInfo(providerId, email, name == null || name.isBlank() ? "카카오 사용자" : name);
    }

    private String post(String url, String formBody, String operation) {
        try {
            return restClient.post()
                    .uri(url)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .body(formBody)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException | ResourceAccessException exception) {
            log.warn("Kakao {} request failed.", operation, exception);
            throw loginFailed(operation + " request failed");
        }
    }

    private JsonNode parse(String responseBody, String operation) {
        try {
            return objectMapper.readTree(responseBody);
        } catch (Exception exception) {
            log.warn("Kakao {} response could not be parsed.", operation);
            throw loginFailed(operation + " response could not be parsed");
        }
    }

    private void requireClientId() {
        if (properties.clientId() == null || properties.clientId().isBlank()) {
            throw new AuthenticationFailedException("OAUTH_NOT_CONFIGURED", "카카오 로그인이 아직 설정되지 않았습니다.");
        }
    }

    private AuthenticationFailedException loginFailed(String reason) {
        log.warn("Kakao OAuth login failed: {}", reason);
        return new AuthenticationFailedException("OAUTH_LOGIN_FAILED", "카카오 로그인에 실패했습니다. 다시 시도해 주세요.");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
