package studio.aroundhub.tunagiftset.coupon;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
import studio.aroundhub.tunagiftset.repository.CartItemRepository;
import studio.aroundhub.tunagiftset.repository.CartRepository;
import studio.aroundhub.tunagiftset.repository.CouponRepository;
import studio.aroundhub.tunagiftset.repository.DeliveryRepository;
import studio.aroundhub.tunagiftset.repository.MemberCouponRepository;
import studio.aroundhub.tunagiftset.repository.MemberRepository;
import studio.aroundhub.tunagiftset.repository.OrderItemRepository;
import studio.aroundhub.tunagiftset.repository.OrderRepository;
import studio.aroundhub.tunagiftset.repository.PaymentRepository;
import studio.aroundhub.tunagiftset.security.JwtTokenProvider;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminCouponIntegrationTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private MemberCouponRepository memberCouponRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Member admin;
    private Member user;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        // The H2 test database is shared across integration test classes within the same JVM run
        // (the Spring context is cached and reused), so member deletion here must also clear
        // every table that FKs into members left over from other test classes, not just this
        // class's own coupon tables.
        memberCouponRepository.deleteAll();
        couponRepository.deleteAll();
        paymentRepository.deleteAll();
        deliveryRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        memberRepository.deleteAll();

        admin = memberRepository.save(new Member(
                "coupon-admin@example.com",
                passwordEncoder.encode("SamplePassword123!"),
                "COUPON ADMIN",
                "01011112222",
                MemberRole.ADMIN,
                MemberStatus.ACTIVE
        ));
        user = memberRepository.save(new Member(
                "coupon-user@example.com",
                passwordEncoder.encode("SamplePassword123!"),
                "COUPON USER",
                "01033334444"
        ));
        adminToken = jwtTokenProvider.createAccessToken(admin);
        userToken = jwtTokenProvider.createAccessToken(user);
    }

    @Test
    void adminCanCreateAFixedAmountCoupon() throws Exception {
        HttpResponse<String> response = post("/api/admin/coupons", couponJson("WELCOME5000", "FIXED_AMOUNT", 5000, 30000, null), adminToken);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo("WELCOME5000");
        assertThat(JsonPath.<String>read(response.body(), "$.discountType")).isEqualTo("FIXED_AMOUNT");
        assertThat(JsonPath.<Integer>read(response.body(), "$.discountValue")).isEqualTo(5000);
        assertThat(JsonPath.<String>read(response.body(), "$.status")).isEqualTo("ACTIVE");
    }

    @Test
    void couponCodeIsNormalizedToUppercaseAndMustBeUnique() throws Exception {
        HttpResponse<String> first = post("/api/admin/coupons", couponJson("lowercase10", "PERCENTAGE", 10, 0, null), adminToken);
        assertThat(first.statusCode()).isEqualTo(201);
        assertThat(JsonPath.<String>read(first.body(), "$.code")).isEqualTo("LOWERCASE10");

        HttpResponse<String> duplicate = post("/api/admin/coupons", couponJson("LOWERCASE10", "PERCENTAGE", 10, 0, null), adminToken);
        assertThat(duplicate.statusCode()).isEqualTo(409);
        assertThat(JsonPath.<String>read(duplicate.body(), "$.code")).isEqualTo("COUPON_CODE_DUPLICATED");
    }

    @Test
    void percentageCouponOver100IsRejected() throws Exception {
        HttpResponse<String> response = post("/api/admin/coupons", couponJson("OVER100", "PERCENTAGE", 150, 0, null), adminToken);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo("INVALID_COUPON_DISCOUNT");
    }

    @Test
    void invalidPeriodIsRejectedWhenValidUntilIsBeforeValidFrom() throws Exception {
        Instant now = Instant.now();
        String json = """
                {
                  "name": "BAD PERIOD",
                  "code": "BADPERIOD",
                  "discountType": "FIXED_AMOUNT",
                  "discountValue": 1000,
                  "minimumOrderAmount": 0,
                  "validFrom": "%s",
                  "validUntil": "%s",
                  "perMemberLimit": 1
                }
                """.formatted(now.toString(), now.minus(1, ChronoUnit.DAYS).toString());

        HttpResponse<String> response = post("/api/admin/coupons", json, adminToken);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo("INVALID_COUPON_PERIOD");
    }

    @Test
    void nonPositiveDiscountValueFailsValidation() throws Exception {
        HttpResponse<String> response = post("/api/admin/coupons", couponJson("ZEROVALUE", "FIXED_AMOUNT", 0, 0, null), adminToken);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void nonAdminCannotAccessAdminCouponApis() throws Exception {
        assertThat(post("/api/admin/coupons", couponJson("USERTRY", "FIXED_AMOUNT", 1000, 0, null), userToken).statusCode()).isEqualTo(403);
        assertThat(get("/api/admin/coupons", userToken).statusCode()).isEqualTo(403);
        assertThat(post("/api/admin/coupons", couponJson("ANONTRY", "FIXED_AMOUNT", 1000, 0, null), null).statusCode()).isEqualTo(401);
        assertThat(get("/api/admin/coupons", null).statusCode()).isEqualTo(401);
    }

    @Test
    void adminCanIssueACouponToAMember() throws Exception {
        String couponId = createCoupon("ISSUEME", "FIXED_AMOUNT", 3000, 0, null, 10, 1);

        HttpResponse<String> response = post("/api/admin/coupons/" + couponId + "/issue", memberIssueJson(user.getId()), adminToken);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(JsonPath.<String>read(response.body(), "$.status")).isEqualTo("AVAILABLE");
        assertThat(memberCouponRepository.countByMemberIdAndCouponId(user.getId(), Long.parseLong(couponId))).isEqualTo(1);
    }

    @Test
    void issueFailsForNonExistentCouponOrMember() throws Exception {
        String couponId = createCoupon("REALCOUPON", "FIXED_AMOUNT", 1000, 0, null, 10, 5);

        assertThat(post("/api/admin/coupons/999999/issue", memberIssueJson(user.getId()), adminToken).statusCode()).isEqualTo(404);
        assertThat(post("/api/admin/coupons/" + couponId + "/issue", memberIssueJson(999999L), adminToken).statusCode()).isEqualTo(404);
    }

    @Test
    void nonAdminCannotIssueCoupons() throws Exception {
        String couponId = createCoupon("NOADMINISSUE", "FIXED_AMOUNT", 1000, 0, null, 10, 5);

        assertThat(post("/api/admin/coupons/" + couponId + "/issue", memberIssueJson(user.getId()), userToken).statusCode()).isEqualTo(403);
    }

    @Test
    void perMemberIssueLimitIsEnforced() throws Exception {
        String couponId = createCoupon("PERMEMBER1", "FIXED_AMOUNT", 1000, 0, null, 10, 1);

        HttpResponse<String> first = post("/api/admin/coupons/" + couponId + "/issue", memberIssueJson(user.getId()), adminToken);
        HttpResponse<String> second = post("/api/admin/coupons/" + couponId + "/issue", memberIssueJson(user.getId()), adminToken);

        assertThat(first.statusCode()).isEqualTo(201);
        assertThat(second.statusCode()).isEqualTo(400);
        assertThat(JsonPath.<String>read(second.body(), "$.code")).isEqualTo("COUPON_MEMBER_LIMIT_EXCEEDED");
    }

    @Test
    void totalIssueLimitIsEnforced() throws Exception {
        String couponId = createCoupon("TOTALLIMIT2", "FIXED_AMOUNT", 1000, 0, null, 2, 1);
        Member second = memberRepository.save(new Member("second-coupon-user@example.com", passwordEncoder.encode("SamplePassword123!"), "SECOND USER", "01055556666"));
        Member third = memberRepository.save(new Member("third-coupon-user@example.com", passwordEncoder.encode("SamplePassword123!"), "THIRD USER", "01077778888"));

        assertThat(post("/api/admin/coupons/" + couponId + "/issue", memberIssueJson(user.getId()), adminToken).statusCode()).isEqualTo(201);
        assertThat(post("/api/admin/coupons/" + couponId + "/issue", memberIssueJson(second.getId()), adminToken).statusCode()).isEqualTo(201);
        HttpResponse<String> thirdResponse = post("/api/admin/coupons/" + couponId + "/issue", memberIssueJson(third.getId()), adminToken);

        assertThat(thirdResponse.statusCode()).isEqualTo(400);
        assertThat(JsonPath.<String>read(thirdResponse.body(), "$.code")).isEqualTo("COUPON_ISSUE_LIMIT_EXCEEDED");
    }

    @Test
    void concurrentIssueRequestsNeverExceedTheTotalIssueLimit() throws Exception {
        int totalLimit = 3;
        String couponId = createCoupon("CONCURRENTLIMIT", "FIXED_AMOUNT", 1000, 0, null, totalLimit, 1);

        int attempts = 8;
        List<Member> members = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            members.add(memberRepository.save(new Member(
                    "concurrent-coupon-" + i + "@example.com",
                    passwordEncoder.encode("SamplePassword123!"),
                    "CONCURRENT USER " + i,
                    "0101234%04d".formatted(i)
            )));
        }

        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        List<Callable<Void>> tasks = new ArrayList<>();
        for (Member candidate : members) {
            tasks.add(() -> {
                startLatch.await(5, TimeUnit.SECONDS);
                HttpResponse<String> response = post("/api/admin/coupons/" + couponId + "/issue", memberIssueJson(candidate.getId()), adminToken);
                if (response.statusCode() == 201) {
                    successCount.incrementAndGet();
                }
                return null;
            });
        }

        var executor = Executors.newFixedThreadPool(attempts);
        var futures = tasks.stream().map(executor::submit).toList();
        startLatch.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        for (var future : futures) {
            future.get();
        }

        assertThat(successCount.get()).isEqualTo(totalLimit);
        assertThat(memberCouponRepository.countByCouponId(Long.parseLong(couponId))).isEqualTo(totalLimit);
    }

    @Test
    void adminCanListCouponsWithPaginationAndStatusFilter() throws Exception {
        createCoupon("LISTED-ACTIVE", "FIXED_AMOUNT", 1000, 0, null, null, 1);
        String inactiveJson = couponJson("LISTED-INACTIVE", "FIXED_AMOUNT", 1000, 0, null).replace("\"status\": \"ACTIVE\"", "\"status\": \"INACTIVE\"");
        post("/api/admin/coupons", inactiveJson, adminToken);

        HttpResponse<String> activeOnly = get("/api/admin/coupons?status=ACTIVE&page=0&size=20", adminToken);
        HttpResponse<String> all = get("/api/admin/coupons?page=0&size=20", adminToken);

        assertThat(activeOnly.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Integer>read(activeOnly.body(), "$.content.length()")).isEqualTo(1);
        assertThat(all.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Integer>read(all.body(), "$.content.length()")).isEqualTo(2);
    }

    private String createCoupon(String code, String discountType, int discountValue, int minimumOrderAmount, Integer maximumDiscountAmount, Integer totalIssueLimit, int perMemberLimit) throws Exception {
        HttpResponse<String> response = post(
                "/api/admin/coupons",
                couponJsonFull(code, discountType, discountValue, minimumOrderAmount, maximumDiscountAmount, totalIssueLimit, perMemberLimit),
                adminToken
        );
        return JsonPath.<Integer>read(response.body(), "$.id").toString();
    }

    private String couponJson(String code, String discountType, int discountValue, int minimumOrderAmount, Integer maximumDiscountAmount) {
        return couponJsonFull(code, discountType, discountValue, minimumOrderAmount, maximumDiscountAmount, null, 1);
    }

    private String couponJsonFull(String code, String discountType, int discountValue, int minimumOrderAmount, Integer maximumDiscountAmount, Integer totalIssueLimit, int perMemberLimit) {
        Instant now = Instant.now();
        return """
                {
                  "name": "SAMPLE %s",
                  "code": "%s",
                  "discountType": "%s",
                  "discountValue": %d,
                  "minimumOrderAmount": %d,
                  "maximumDiscountAmount": %s,
                  "validFrom": "%s",
                  "validUntil": "%s",
                  "status": "ACTIVE",
                  "totalIssueLimit": %s,
                  "perMemberLimit": %d
                }
                """.formatted(
                code,
                code,
                discountType,
                discountValue,
                minimumOrderAmount,
                maximumDiscountAmount == null ? "null" : maximumDiscountAmount,
                now.minus(1, ChronoUnit.DAYS),
                now.plus(30, ChronoUnit.DAYS),
                totalIssueLimit == null ? "null" : totalIssueLimit,
                perMemberLimit
        );
    }

    private String memberIssueJson(Long memberId) {
        return """
                { "memberId": %d }
                """.formatted(memberId);
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(uri(path)).GET();
        applyToken(builder, token);
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
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
