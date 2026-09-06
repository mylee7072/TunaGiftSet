package studio.aroundhub.tunagiftset.coupon;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import studio.aroundhub.tunagiftset.entity.Brand;
import studio.aroundhub.tunagiftset.entity.Cart;
import studio.aroundhub.tunagiftset.entity.CartItem;
import studio.aroundhub.tunagiftset.entity.Category;
import studio.aroundhub.tunagiftset.entity.Coupon;
import studio.aroundhub.tunagiftset.entity.Member;
import studio.aroundhub.tunagiftset.entity.MemberCoupon;
import studio.aroundhub.tunagiftset.entity.Product;
import studio.aroundhub.tunagiftset.entity.type.CouponStatus;
import studio.aroundhub.tunagiftset.entity.type.DiscountType;
import studio.aroundhub.tunagiftset.entity.type.MemberCouponStatus;
import studio.aroundhub.tunagiftset.entity.type.ProductStatus;
import studio.aroundhub.tunagiftset.payment.FakePaymentGatewayClient;
import studio.aroundhub.tunagiftset.repository.BrandRepository;
import studio.aroundhub.tunagiftset.repository.CartItemRepository;
import studio.aroundhub.tunagiftset.repository.CartRepository;
import studio.aroundhub.tunagiftset.repository.CategoryRepository;
import studio.aroundhub.tunagiftset.repository.CouponRepository;
import studio.aroundhub.tunagiftset.repository.DeliveryRepository;
import studio.aroundhub.tunagiftset.repository.InventoryHistoryRepository;
import studio.aroundhub.tunagiftset.repository.MemberCouponRepository;
import studio.aroundhub.tunagiftset.repository.MemberRepository;
import studio.aroundhub.tunagiftset.repository.OrderItemRepository;
import studio.aroundhub.tunagiftset.repository.OrderRepository;
import studio.aroundhub.tunagiftset.repository.PaymentRepository;
import studio.aroundhub.tunagiftset.repository.ProductImageRepository;
import studio.aroundhub.tunagiftset.repository.ProductRepository;
import studio.aroundhub.tunagiftset.security.JwtTokenProvider;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CouponOrderIntegrationTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private FakePaymentGatewayClient fakePaymentGatewayClient;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private InventoryHistoryRepository inventoryHistoryRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private MemberCouponRepository memberCouponRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Member member;
    private Member anotherMember;
    private String memberToken;
    private String anotherToken;
    private Brand brand;
    private Category category;

    @BeforeEach
    void setUp() {
        fakePaymentGatewayClient.reset();

        paymentRepository.deleteAll();
        deliveryRepository.deleteAll();
        orderItemRepository.deleteAll();
        memberCouponRepository.deleteAll();
        couponRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        productImageRepository.deleteAll();
        inventoryHistoryRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        brandRepository.deleteAll();
        memberRepository.deleteAll();

        member = memberRepository.save(new Member(
                "coupon-order-user@example.com",
                passwordEncoder.encode("SamplePassword123!"),
                "SAMPLE USER",
                "01012345678"
        ));
        anotherMember = memberRepository.save(new Member(
                "another-coupon-order-user@example.com",
                passwordEncoder.encode("SamplePassword123!"),
                "ANOTHER SAMPLE USER",
                "01087654321"
        ));
        memberToken = jwtTokenProvider.createAccessToken(member);
        anotherToken = jwtTokenProvider.createAccessToken(anotherMember);
        brand = brandRepository.save(new Brand("COUPON-ORDER", "COUPON ORDER BRAND", true));
        category = categoryRepository.save(new Category(null, "COUPON ORDER CATEGORY", 1, true));
    }

    // ---------------------------------------------------------------- preview

    @Test
    void previewWithoutCouponReturnsProductAndShippingOnly() throws Exception {
        Product product = saveProduct(BigDecimal.valueOf(60000), 10);
        CartItem item = addCartItem(member, product, 1);

        HttpResponse<String> response = post("/api/checkout/preview", previewJson(List.of(item.getId()), null), memberToken);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Integer>read(response.body(), "$.productAmount")).isEqualTo(60000);
        assertThat(JsonPath.<Integer>read(response.body(), "$.couponDiscountAmount")).isZero();
        assertThat(JsonPath.<Integer>read(response.body(), "$.shippingFee")).isZero();
        assertThat(JsonPath.<Integer>read(response.body(), "$.totalAmount")).isEqualTo(60000);
    }

    @Test
    void previewWithFixedAmountCouponAppliesDiscount() throws Exception {
        Product product = saveProduct(BigDecimal.valueOf(60000), 10);
        CartItem item = addCartItem(member, product, 1);
        MemberCoupon memberCoupon = issueCoupon(member, fixedAmountCoupon(5000, 0, null));

        HttpResponse<String> response = post("/api/checkout/preview", previewJson(List.of(item.getId()), memberCoupon.getId()), memberToken);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Integer>read(response.body(), "$.couponDiscountAmount")).isEqualTo(5000);
        assertThat(JsonPath.<Integer>read(response.body(), "$.totalAmount")).isEqualTo(55000);
    }

    @Test
    void previewWithPercentageCouponAppliesMaximumDiscountCap() throws Exception {
        Product product = saveProduct(BigDecimal.valueOf(200000), 10);
        CartItem item = addCartItem(member, product, 1);
        MemberCoupon memberCoupon = issueCoupon(member, percentageCoupon(10, 0, 10000));

        HttpResponse<String> response = post("/api/checkout/preview", previewJson(List.of(item.getId()), memberCoupon.getId()), memberToken);

        assertThat(response.statusCode()).isEqualTo(200);
        // 10% of 200000 would be 20000, capped to 10000.
        assertThat(JsonPath.<Integer>read(response.body(), "$.couponDiscountAmount")).isEqualTo(10000);
        assertThat(JsonPath.<Integer>read(response.body(), "$.totalAmount")).isEqualTo(190000);
    }

    @Test
    void availableCouponsListMarksCouponBelowMinimumOrderAmountAsUnusable() throws Exception {
        Product product = saveProduct(BigDecimal.valueOf(10000), 10);
        CartItem item = addCartItem(member, product, 1);
        issueCoupon(member, fixedAmountCoupon(5000, 50000, null));

        HttpResponse<String> response = post("/api/checkout/preview", previewJson(List.of(item.getId()), null), memberToken);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Boolean>read(response.body(), "$.coupons[0].usable")).isFalse();
        assertThat(JsonPath.<String>read(response.body(), "$.coupons[0].unavailableReason")).isEqualTo("COUPON_MINIMUM_ORDER_NOT_MET");
    }

    @Test
    void previewRejectsSelectingACouponBelowMinimumOrderAmount() throws Exception {
        Product product = saveProduct(BigDecimal.valueOf(10000), 10);
        CartItem item = addCartItem(member, product, 1);
        MemberCoupon memberCoupon = issueCoupon(member, fixedAmountCoupon(5000, 50000, null));

        HttpResponse<String> response = post("/api/checkout/preview", previewJson(List.of(item.getId()), memberCoupon.getId()), memberToken);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo("COUPON_MINIMUM_ORDER_NOT_MET");
    }

    @Test
    void previewRejectsAnotherMembersCoupon() throws Exception {
        Product product = saveProduct(BigDecimal.valueOf(60000), 10);
        CartItem item = addCartItem(member, product, 1);
        MemberCoupon othersCoupon = issueCoupon(anotherMember, fixedAmountCoupon(5000, 0, null));

        HttpResponse<String> response = post("/api/checkout/preview", previewJson(List.of(item.getId()), othersCoupon.getId()), memberToken);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo("COUPON_NOT_FOUND");
    }

    @Test
    void previewRejectsExpiredCoupon() throws Exception {
        Product product = saveProduct(BigDecimal.valueOf(60000), 10);
        CartItem item = addCartItem(member, product, 1);
        Coupon expired = couponRepository.save(new Coupon(
                "EXPIRED", "EXPIRED" + System.nanoTime(), DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(1000),
                BigDecimal.ZERO, null,
                Instant.now().minus(10, ChronoUnit.DAYS), Instant.now().minus(1, ChronoUnit.SECONDS),
                CouponStatus.ACTIVE, null, 1
        ));
        MemberCoupon memberCoupon = memberCouponRepository.save(new MemberCoupon(member, expired, Instant.now().minus(10, ChronoUnit.DAYS)));

        HttpResponse<String> response = post("/api/checkout/preview", previewJson(List.of(item.getId()), memberCoupon.getId()), memberToken);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo("COUPON_EXPIRED");
    }

    @Test
    void previewRejectsNotYetStartedCoupon() throws Exception {
        Product product = saveProduct(BigDecimal.valueOf(60000), 10);
        CartItem item = addCartItem(member, product, 1);
        Coupon future = couponRepository.save(new Coupon(
                "FUTURE", "FUTURE" + System.nanoTime(), DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(1000),
                BigDecimal.ZERO, null,
                Instant.now().plus(1, ChronoUnit.DAYS), Instant.now().plus(10, ChronoUnit.DAYS),
                CouponStatus.ACTIVE, null, 1
        ));
        MemberCoupon memberCoupon = memberCouponRepository.save(new MemberCoupon(member, future, Instant.now()));

        HttpResponse<String> response = post("/api/checkout/preview", previewJson(List.of(item.getId()), memberCoupon.getId()), memberToken);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo("COUPON_NOT_STARTED");
    }

    // ---------------------------------------------------------------- order creation

    @Test
    void creatingOrderWithCouponAppliesDiscountAndReservesTheCoupon() throws Exception {
        Product product = saveProduct(BigDecimal.valueOf(60000), 10);
        CartItem item = addCartItem(member, product, 1);
        MemberCoupon memberCoupon = issueCoupon(member, fixedAmountCoupon(5000, 0, null));

        HttpResponse<String> response = post("/api/orders", orderJson(List.of(item.getId()), memberCoupon.getId()), memberToken);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Integer>read(response.body(), "$.productAmount")).isEqualTo(60000);
        assertThat(JsonPath.<Integer>read(response.body(), "$.couponDiscountAmount")).isEqualTo(5000);
        assertThat(JsonPath.<Integer>read(response.body(), "$.totalAmount")).isEqualTo(55000);
        assertThat(JsonPath.<String>read(response.body(), "$.couponCodeSnapshot")).isEqualTo(memberCoupon.getCoupon().getCode());

        MemberCoupon reserved = memberCouponRepository.findById(memberCoupon.getId()).orElseThrow();
        assertThat(reserved.getStatus()).isEqualTo(MemberCouponStatus.RESERVED);
        assertThat(reserved.getReservedOrder()).isNotNull();
    }

    @Test
    void couponSnapshotSurvivesLaterCouponPolicyChanges() throws Exception {
        Product product = saveProduct(BigDecimal.valueOf(60000), 10);
        CartItem item = addCartItem(member, product, 1);
        Coupon coupon = fixedAmountCoupon(5000, 0, null);
        MemberCoupon memberCoupon = issueCoupon(member, coupon);

        String orderNumber = JsonPath.read(post("/api/orders", orderJson(List.of(item.getId()), memberCoupon.getId()), memberToken).body(), "$.orderNumber");

        Coupon reloaded = couponRepository.findById(coupon.getId()).orElseThrow();
        reloaded.update(reloaded.getName(), reloaded.getDiscountType(), BigDecimal.valueOf(20000), reloaded.getMinimumOrderAmount(),
                reloaded.getMaximumDiscountAmount(), reloaded.getValidFrom(), reloaded.getValidUntil(), reloaded.getStatus(),
                reloaded.getTotalIssueLimit(), reloaded.getPerMemberLimit());
        couponRepository.save(reloaded);

        HttpResponse<String> detail = get("/api/orders/" + orderNumber, memberToken);
        assertThat(JsonPath.<Integer>read(detail.body(), "$.couponDiscountAmount")).isEqualTo(5000);
        assertThat(JsonPath.<Integer>read(detail.body(), "$.totalAmount")).isEqualTo(55000);
    }

    @Test
    void creatingOrderFailsWhenCouponIsAlreadyReservedByAnotherOrder() throws Exception {
        Product product1 = saveProduct(BigDecimal.valueOf(60000), 10);
        Product product2 = saveProduct(BigDecimal.valueOf(60000), 10);
        CartItem item1 = addCartItem(member, product1, 1);
        CartItem item2 = addCartItem(member, product2, 1);
        MemberCoupon memberCoupon = issueCoupon(member, fixedAmountCoupon(5000, 0, null));

        HttpResponse<String> first = post("/api/orders", orderJson(List.of(item1.getId()), memberCoupon.getId()), memberToken);
        HttpResponse<String> second = post("/api/orders", orderJson(List.of(item2.getId()), memberCoupon.getId()), memberToken);

        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(second.statusCode()).isEqualTo(400);
        assertThat(JsonPath.<String>read(second.body(), "$.code")).isEqualTo("COUPON_ALREADY_RESERVED");
        assertThat(orderRepository.findAll()).hasSize(1);
    }

    @Test
    void creatingOrderFailsWhenCouponIsAlreadyUsed() throws Exception {
        Product product1 = saveProduct(BigDecimal.valueOf(60000), 10);
        Product product2 = saveProduct(BigDecimal.valueOf(60000), 10);
        CartItem item1 = addCartItem(member, product1, 1);
        MemberCoupon memberCoupon = issueCoupon(member, fixedAmountCoupon(5000, 0, null));
        String orderNumber = JsonPath.read(post("/api/orders", orderJson(List.of(item1.getId()), memberCoupon.getId()), memberToken).body(), "$.orderNumber");
        confirm(orderNumber, "used-coupon-key", 55000);

        CartItem item2 = addCartItem(member, product2, 1);
        HttpResponse<String> secondOrder = post("/api/orders", orderJson(List.of(item2.getId()), memberCoupon.getId()), memberToken);

        assertThat(secondOrder.statusCode()).isEqualTo(400);
        assertThat(JsonPath.<String>read(secondOrder.body(), "$.code")).isEqualTo("COUPON_ALREADY_USED");
    }

    @Test
    void creatingOrderRejectsAnotherMembersCouponEvenWhenIdIsGuessed() throws Exception {
        Product product = saveProduct(BigDecimal.valueOf(60000), 10);
        CartItem item = addCartItem(member, product, 1);
        MemberCoupon othersCoupon = issueCoupon(anotherMember, fixedAmountCoupon(5000, 0, null));

        HttpResponse<String> response = post("/api/orders", orderJson(List.of(item.getId()), othersCoupon.getId()), memberToken);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo("COUPON_NOT_FOUND");
        assertThat(orderRepository.findAll()).isEmpty();
        MemberCoupon reloaded = memberCouponRepository.findById(othersCoupon.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(MemberCouponStatus.AVAILABLE);
    }

    @Test
    void zeroFinalAmountOrderIsRejectedAndNothingIsPersisted() throws Exception {
        // 100% off on a free-shipping-threshold order drives totalAmount to exactly zero.
        Product product = saveProduct(BigDecimal.valueOf(50000), 10);
        CartItem item = addCartItem(member, product, 1);
        MemberCoupon memberCoupon = issueCoupon(member, percentageCoupon(100, 0, null));

        HttpResponse<String> response = post("/api/orders", orderJson(List.of(item.getId()), memberCoupon.getId()), memberToken);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo("ZERO_AMOUNT_ORDER_NOT_SUPPORTED");
        assertThat(orderRepository.findAll()).isEmpty();
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(10);
        assertThat(memberCouponRepository.findById(memberCoupon.getId()).orElseThrow().getStatus()).isEqualTo(MemberCouponStatus.AVAILABLE);
    }

    // ---------------------------------------------------------------- lifecycle: use / restore

    @Test
    void paymentConfirmationMarksTheReservedCouponAsUsed() throws Exception {
        Product product = saveProduct(BigDecimal.valueOf(60000), 10);
        CartItem item = addCartItem(member, product, 1);
        MemberCoupon memberCoupon = issueCoupon(member, fixedAmountCoupon(5000, 0, null));
        String orderNumber = JsonPath.read(post("/api/orders", orderJson(List.of(item.getId()), memberCoupon.getId()), memberToken).body(), "$.orderNumber");

        HttpResponse<String> confirmResponse = confirm(orderNumber, "coupon-confirm-key", 55000);

        assertThat(confirmResponse.statusCode()).isEqualTo(200);
        MemberCoupon used = memberCouponRepository.findById(memberCoupon.getId()).orElseThrow();
        assertThat(used.getStatus()).isEqualTo(MemberCouponStatus.USED);
        assertThat(used.getUsedAt()).isNotNull();
    }

    @Test
    void cancelingBeforePaymentRestoresTheReservedCouponToAvailable() throws Exception {
        Product product = saveProduct(BigDecimal.valueOf(60000), 10);
        CartItem item = addCartItem(member, product, 1);
        MemberCoupon memberCoupon = issueCoupon(member, fixedAmountCoupon(5000, 0, null));
        String orderNumber = JsonPath.read(post("/api/orders", orderJson(List.of(item.getId()), memberCoupon.getId()), memberToken).body(), "$.orderNumber");

        HttpResponse<String> cancelResponse = post("/api/orders/" + orderNumber + "/cancel", "{}", memberToken);

        assertThat(cancelResponse.statusCode()).isEqualTo(200);
        MemberCoupon restored = memberCouponRepository.findById(memberCoupon.getId()).orElseThrow();
        assertThat(restored.getStatus()).isEqualTo(MemberCouponStatus.AVAILABLE);
        assertThat(restored.getReservedOrder()).isNull();
    }

    @Test
    void cancelingAfterPaymentRestoresTheUsedCouponToAvailableExactlyOnce() throws Exception {
        Product product = saveProduct(BigDecimal.valueOf(60000), 10);
        CartItem item = addCartItem(member, product, 1);
        MemberCoupon memberCoupon = issueCoupon(member, fixedAmountCoupon(5000, 0, null));
        String orderNumber = JsonPath.read(post("/api/orders", orderJson(List.of(item.getId()), memberCoupon.getId()), memberToken).body(), "$.orderNumber");
        confirm(orderNumber, "cancel-after-pay-key", 55000);

        HttpResponse<String> firstCancel = post("/api/orders/" + orderNumber + "/cancel", "{}", memberToken);
        HttpResponse<String> secondCancel = post("/api/orders/" + orderNumber + "/cancel", "{}", memberToken);

        assertThat(firstCancel.statusCode()).isEqualTo(200);
        assertThat(secondCancel.statusCode()).isEqualTo(200);
        MemberCoupon restored = memberCouponRepository.findById(memberCoupon.getId()).orElseThrow();
        assertThat(restored.getStatus()).isEqualTo(MemberCouponStatus.AVAILABLE);
        assertThat(restored.getUsedOrder()).isNull();
    }

    @Test
    void restoredCouponCanBeReusedOnANewOrder() throws Exception {
        Product product1 = saveProduct(BigDecimal.valueOf(60000), 10);
        Product product2 = saveProduct(BigDecimal.valueOf(60000), 10);
        CartItem item1 = addCartItem(member, product1, 1);
        MemberCoupon memberCoupon = issueCoupon(member, fixedAmountCoupon(5000, 0, null));
        String orderNumber = JsonPath.read(post("/api/orders", orderJson(List.of(item1.getId()), memberCoupon.getId()), memberToken).body(), "$.orderNumber");
        post("/api/orders/" + orderNumber + "/cancel", "{}", memberToken);

        CartItem item2 = addCartItem(member, product2, 1);
        HttpResponse<String> secondOrder = post("/api/orders", orderJson(List.of(item2.getId()), memberCoupon.getId()), memberToken);

        assertThat(secondOrder.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Integer>read(secondOrder.body(), "$.couponDiscountAmount")).isEqualTo(5000);
    }

    // ---------------------------------------------------------------- concurrency

    @Test
    void concurrentOrdersForTheSameCouponResultInExactlyOneReservation() throws Exception {
        Product product1 = saveProduct(BigDecimal.valueOf(60000), 10);
        Product product2 = saveProduct(BigDecimal.valueOf(60000), 10);
        CartItem item1 = addCartItem(member, product1, 1);
        CartItem item2 = addCartItem(member, product2, 1);
        MemberCoupon memberCoupon = issueCoupon(member, fixedAmountCoupon(5000, 0, null));

        CountDownLatch startLatch = new CountDownLatch(1);
        Callable<Integer> orderA = () -> {
            startLatch.await(5, TimeUnit.SECONDS);
            return post("/api/orders", orderJson(List.of(item1.getId()), memberCoupon.getId()), memberToken).statusCode();
        };
        Callable<Integer> orderB = () -> {
            startLatch.await(5, TimeUnit.SECONDS);
            return post("/api/orders", orderJson(List.of(item2.getId()), memberCoupon.getId()), memberToken).statusCode();
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Integer> futureA = executor.submit(orderA);
        Future<Integer> futureB = executor.submit(orderB);
        startLatch.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        List<Integer> statusCodes = List.of(futureA.get(), futureB.get());
        long successCount = statusCodes.stream().filter(code -> code == 200).count();

        assertThat(successCount).isEqualTo(1);
        assertThat(orderRepository.findAll()).hasSize(1);
        MemberCoupon finalState = memberCouponRepository.findById(memberCoupon.getId()).orElseThrow();
        assertThat(finalState.getStatus()).isEqualTo(MemberCouponStatus.RESERVED);
    }

    // ---------------------------------------------------------------- my coupons listing

    @Test
    void myCouponsListOnlyShowsTheOwnersOwnCoupons() throws Exception {
        issueCoupon(member, fixedAmountCoupon(1000, 0, null));
        issueCoupon(anotherMember, fixedAmountCoupon(2000, 0, null));

        HttpResponse<String> response = get("/api/members/me/coupons", memberToken);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Integer>read(response.body(), "$.content.length()")).isEqualTo(1);
        assertThat(JsonPath.<Integer>read(response.body(), "$.content[0].discountValue")).isEqualTo(1000);
    }

    @Test
    void myCouponsRequiresAuthentication() throws Exception {
        assertThat(get("/api/members/me/coupons", null).statusCode()).isEqualTo(401);
    }

    // ---------------------------------------------------------------- helpers

    private Coupon fixedAmountCoupon(int discountValue, int minimumOrderAmount, Integer maximumDiscountAmount) {
        return couponRepository.save(new Coupon(
                "FIXED COUPON", "FIXED" + System.nanoTime(), DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(discountValue),
                BigDecimal.valueOf(minimumOrderAmount), maximumDiscountAmount == null ? null : BigDecimal.valueOf(maximumDiscountAmount),
                Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(30, ChronoUnit.DAYS),
                CouponStatus.ACTIVE, null, 5
        ));
    }

    private Coupon percentageCoupon(int discountValue, int minimumOrderAmount, Integer maximumDiscountAmount) {
        return couponRepository.save(new Coupon(
                "PERCENT COUPON", "PERCENT" + System.nanoTime(), DiscountType.PERCENTAGE, BigDecimal.valueOf(discountValue),
                BigDecimal.valueOf(minimumOrderAmount), maximumDiscountAmount == null ? null : BigDecimal.valueOf(maximumDiscountAmount),
                Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(30, ChronoUnit.DAYS),
                CouponStatus.ACTIVE, null, 5
        ));
    }

    private MemberCoupon issueCoupon(Member owner, Coupon coupon) {
        return memberCouponRepository.save(new MemberCoupon(owner, coupon, Instant.now()));
    }

    private Product saveProduct(BigDecimal salePrice, int stockQuantity) {
        return productRepository.save(new Product(
                brand,
                category,
                "COUPON-ORDER-" + System.nanoTime(),
                "SAMPLE COUPON PRODUCT",
                "SAMPLE",
                "SAMPLE COUPON PRODUCT DATA",
                salePrice,
                salePrice,
                stockQuantity,
                ProductStatus.ACTIVE,
                false
        ));
    }

    private CartItem addCartItem(Member owner, Product product, int quantity) {
        Cart cart = cartRepository.findByMemberId(owner.getId())
                .orElseGet(() -> cartRepository.save(new Cart(owner)));
        return cartItemRepository.save(new CartItem(cart, product, quantity));
    }

    private String previewJson(List<Long> cartItemIds, Long memberCouponId) {
        return """
                {
                  "cartItemIds": %s,
                  "memberCouponId": %s
                }
                """.formatted(cartItemIds, memberCouponId == null ? "null" : memberCouponId);
    }

    private String orderJson(List<Long> cartItemIds, Long memberCouponId) {
        return """
                {
                  "cartItemIds": %s,
                  "memberCouponId": %s,
                  "recipientName": "SAMPLE RECIPIENT",
                  "recipientPhone": "01012345678",
                  "zipCode": "12345",
                  "address1": "SEOUL SAMPLE ADDRESS",
                  "address2": "101",
                  "deliveryMessage": "LEAVE AT DOOR"
                }
                """.formatted(cartItemIds, memberCouponId == null ? "null" : memberCouponId);
    }

    private HttpResponse<String> confirm(String orderNumber, String paymentKey, int amount) throws Exception {
        String json = """
                {
                  "paymentKey": "%s",
                  "orderId": "%s",
                  "amount": %d
                }
                """.formatted(paymentKey, orderNumber, amount);
        return post("/api/payments/confirm", json, memberToken);
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
