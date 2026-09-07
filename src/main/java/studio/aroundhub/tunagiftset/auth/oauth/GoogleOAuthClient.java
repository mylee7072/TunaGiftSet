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

/** Talks to Google's OAuth endpoints (developers.google.com/identity/protocols/oauth2/web-server). */
@Component
public class GoogleOAuthClient implements OAuthClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthClient.class);
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GoogleOAuthProperties properties;

    public GoogleOAuthClient(GoogleOAuthProperties properties) {
        this.properties = properties;
    }

    @Override
    public MemberProvider providerType() {
        return MemberProvider.GOOGLE;
    }

    @Override
    public OAuthUserInfo authenticate(String code, String redirectUri) {
        requireConfigured();
        String accessToken = exchangeToken(code, redirectUri);
        return fetchUserInfo(accessToken);
    }

    private String exchangeToken(String code, String redirectUri) {
        String form = "grant_type=authorization_code"
                + "&client_id=" + encode(properties.clientId())
                + "&client_secret=" + encode(properties.clientSecret())
                + "&redirect_uri=" + encode(redirectUri)
                + "&code=" + encode(code);

        JsonNode root = parse(post(TOKEN_URL, form, "token exchange"), "token exchange");
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
            log.warn("Google user info request failed.", exception);
            throw loginFailed("user info request failed");
        }

        JsonNode root = parse(responseBody, "user info");
        String providerId = root.path("sub").asString(null);
        String email = root.path("email").asString(null);
        String name = root.path("name").asString(null);

        if (providerId == null) {
            throw loginFailed("user info missing sub");
        }
        if (email == null || email.isBlank()) {
            throw new AuthenticationFailedException(
                    "OAUTH_EMAIL_REQUIRED",
                    "구글 계정에서 이메일 제공에 동의해야 로그인할 수 있습니다."
            );
        }
        return new OAuthUserInfo(providerId, email, name == null || name.isBlank() ? "구글 사용자" : name);
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
            log.warn("Google {} request failed.", operation, exception);
            throw loginFailed(operation + " request failed");
        }
    }

    private JsonNode parse(String responseBody, String operation) {
        try {
            return objectMapper.readTree(responseBody);
        } catch (Exception exception) {
            log.warn("Google {} response could not be parsed.", operation);
            throw loginFailed(operation + " response could not be parsed");
        }
    }

    private void requireConfigured() {
        if (properties.clientId() == null || properties.clientId().isBlank()
                || properties.clientSecret() == null || properties.clientSecret().isBlank()) {
            throw new AuthenticationFailedException("OAUTH_NOT_CONFIGURED", "구글 로그인이 아직 설정되지 않았습니다.");
        }
    }

    private AuthenticationFailedException loginFailed(String reason) {
        log.warn("Google OAuth login failed: {}", reason);
        return new AuthenticationFailedException("OAUTH_LOGIN_FAILED", "구글 로그인에 실패했습니다. 다시 시도해 주세요.");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
