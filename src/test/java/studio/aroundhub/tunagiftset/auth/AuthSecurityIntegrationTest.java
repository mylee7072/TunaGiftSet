package studio.aroundhub.tunagiftset.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import studio.aroundhub.tunagiftset.entity.Member;
import studio.aroundhub.tunagiftset.entity.type.MemberRole;
import studio.aroundhub.tunagiftset.entity.type.MemberStatus;
import studio.aroundhub.tunagiftset.repository.BrandRepository;
import studio.aroundhub.tunagiftset.repository.MemberRepository;
import studio.aroundhub.tunagiftset.security.JwtTokenProvider;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthSecurityIntegrationTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        brandRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    void signup() throws Exception {
        HttpResponse<String> response = post("/api/auth/signup", """
                {
                  "email": "sample-signup@example.com",
                  "password": "SamplePassword123!",
                  "name": "샘플회원",
                  "phone": "01012345678"
                }
                """);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(JsonPath.<String>read(response.body(), "$.email")).isEqualTo("sample-signup@example.com");
        assertThat(JsonPath.<String>read(response.body(), "$.role")).isEqualTo("USER");
        assertThat(response.body()).doesNotContain("password");

        Optional<Member> savedMember = memberRepository.findByEmail("sample-signup@example.com");
        assertThat(savedMember).isPresent();
        assertThat(savedMember.get().getPassword()).isNotEqualTo("SamplePassword123!");
        assertThat(passwordEncoder.matches("SamplePassword123!", savedMember.get().getPassword())).isTrue();
    }

    @Test
    void signupFailsWhenEmailDuplicated() throws Exception {
        memberRepository.save(new Member(
                "duplicated@example.com",
                passwordEncoder.encode("SamplePassword123!"),
                "샘플회원",
                "01012345678"
        ));

        HttpResponse<String> response = post("/api/auth/signup", """
                {
                  "email": "duplicated@example.com",
                  "password": "SamplePassword123!",
                  "name": "샘플회원",
                  "phone": "01012345678"
                }
                """);

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo("EMAIL_DUPLICATED");
    }

    @Test
    void signupFailsWhenEmailInvalid() throws Exception {
        HttpResponse<String> response = post("/api/auth/signup", """
                {
                  "email": "invalid-email",
                  "password": "SamplePassword123!",
                  "name": "샘플회원",
                  "phone": "01012345678"
                }
                """);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void signupFailsWhenPasswordWeak() throws Exception {
        HttpResponse<String> response = post("/api/auth/signup", """
                {
                  "email": "weak-password@example.com",
                  "password": "short",
                  "name": "샘플회원",
                  "phone": "01012345678"
                }
                """);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void login() throws Exception {
        memberRepository.save(new Member(
                "login@example.com",
                passwordEncoder.encode("SamplePassword123!"),
                "샘플회원",
                "01012345678"
        ));

        HttpResponse<String> response = post("/api/auth/login", """
                {
                  "email": "login@example.com",
                  "password": "SamplePassword123!"
                }
                """);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(response.body(), "$.tokenType")).isEqualTo("Bearer");
        assertThat(JsonPath.<String>read(response.body(), "$.accessToken")).isNotBlank();
        assertThat(JsonPath.<String>read(response.body(), "$.member.email")).isEqualTo("login@example.com");
        assertThat(response.body()).doesNotContain("password");
    }

    @Test
    void loginFailsWhenPasswordInvalid() throws Exception {
        memberRepository.save(new Member(
                "wrong-password@example.com",
                passwordEncoder.encode("SamplePassword123!"),
                "샘플회원",
                "01012345678"
        ));

        HttpResponse<String> response = post("/api/auth/login", """
                {
                  "email": "wrong-password@example.com",
                  "password": "WrongPassword123!"
                }
                """);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo("LOGIN_FAILED");
    }

    @Test
    void loginFailsWhenMemberDoesNotExist() throws Exception {
        HttpResponse<String> response = post("/api/auth/login", """
                {
                  "email": "missing@example.com",
                  "password": "SamplePassword123!"
                }
                """);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo("LOGIN_FAILED");
    }

    @Test
    void publicProductApiAllowsAnonymousAccess() throws Exception {
        HttpResponse<String> response = get("/api/products", null);

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        HttpResponse<String> response = get("/api/members/me", null);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo("UNAUTHORIZED");
    }

    @Test
    void userCannotAccessAdminApi() throws Exception {
        Member user = memberRepository.save(new Member(
                "user@example.com",
                passwordEncoder.encode("SamplePassword123!"),
                "샘플회원",
                "01012345678"
        ));
        String token = jwtTokenProvider.createAccessToken(user);

        HttpResponse<String> response = post("/api/admin/brands", """
                {
                  "name": "SAMPLE",
                  "displayName": "SAMPLE BRAND",
                  "active": true
                }
                """, token);

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo("FORBIDDEN");
    }

    @Test
    void adminCanAccessAdminApi() throws Exception {
        Member admin = memberRepository.save(new Member(
                "admin@example.com",
                passwordEncoder.encode("SamplePassword123!"),
                "샘플관리자",
                "01012345678",
                MemberRole.ADMIN,
                MemberStatus.ACTIVE
        ));
        String token = jwtTokenProvider.createAccessToken(admin);

        HttpResponse<String> response = post("/api/admin/brands", """
                {
                  "name": "SAMPLE-ADMIN",
                  "displayName": "SAMPLE BRAND",
                  "active": true
                }
                """, token);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(JsonPath.<String>read(response.body(), "$.name")).isEqualTo("SAMPLE-ADMIN");
    }

    @Test
    void meReturnsAuthenticatedMember() throws Exception {
        Member user = memberRepository.save(new Member(
                "me@example.com",
                passwordEncoder.encode("SamplePassword123!"),
                "샘플회원",
                "01012345678"
        ));
        String token = jwtTokenProvider.createAccessToken(user);

        HttpResponse<String> response = get("/api/members/me", token);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(response.body(), "$.email")).isEqualTo("me@example.com");
        assertThat(JsonPath.<String>read(response.body(), "$.role")).isEqualTo("USER");
        assertThat(response.body()).doesNotContain("password");
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri(path))
                .GET();
        applyToken(builder, token);
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String json) throws Exception {
        return post(path, json, null);
    }

    private HttpResponse<String> post(String path, String json, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri(path))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(json));
        applyToken(builder, token);
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private void applyToken(HttpRequest.Builder builder, String token) {
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
