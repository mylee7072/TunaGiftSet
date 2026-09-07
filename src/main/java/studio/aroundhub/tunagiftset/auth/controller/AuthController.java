package studio.aroundhub.tunagiftset.auth.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import studio.aroundhub.tunagiftset.auth.dto.LoginRequest;
import studio.aroundhub.tunagiftset.auth.dto.LoginResponse;
import studio.aroundhub.tunagiftset.auth.dto.OAuthLoginRequest;
import studio.aroundhub.tunagiftset.auth.dto.SignupRequest;
import studio.aroundhub.tunagiftset.auth.oauth.OAuthClient;
import studio.aroundhub.tunagiftset.auth.oauth.OAuthUserInfo;
import studio.aroundhub.tunagiftset.auth.service.AuthService;
import studio.aroundhub.tunagiftset.exception.InvalidRequestException;
import studio.aroundhub.tunagiftset.member.dto.MemberResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final Map<String, OAuthClient> oauthClientsByPath;

    public AuthController(AuthService authService, List<OAuthClient> oauthClients) {
        this.authService = authService;
        this.oauthClientsByPath = oauthClients.stream()
                .collect(Collectors.toMap(
                        client -> client.providerType().name().toLowerCase(Locale.ROOT),
                        Function.identity()
                ));
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/oauth/{provider}")
    public LoginResponse oauthLogin(@PathVariable String provider, @Valid @RequestBody OAuthLoginRequest request) {
        OAuthClient client = resolveClient(provider);
        OAuthUserInfo userInfo = client.authenticate(request.code(), request.redirectUri());
        return authService.oauthLogin(client.providerType(), userInfo);
    }

    private OAuthClient resolveClient(String provider) {
        OAuthClient client = oauthClientsByPath.get(provider.toLowerCase(Locale.ROOT));
        if (client == null) {
            throw new InvalidRequestException("UNSUPPORTED_OAUTH_PROVIDER", "지원하지 않는 로그인 방식입니다.");
        }
        return client;
    }
}
