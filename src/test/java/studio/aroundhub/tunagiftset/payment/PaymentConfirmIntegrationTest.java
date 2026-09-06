package studio.aroundhub.tunagiftset.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
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
import org.springframework.test.util.ReflectionTestUtils;
import studio.aroundhub.tunagiftset.entity.Brand;
import studio.aroundhub.tunagiftset.entity.Cart;
import studio.aroundhub.tunagiftset.entity.CartItem;
import studio.aroundhub.tunagiftset.entity.Category;
import studio.aroundhub.tunagiftset.entity.Member;
import studio.aroundhub.tunagiftset.entity.Order;
import studio.aroundhub.tunagiftset.entity.Payment;
import studio.aroundhub.tunagiftset.entity.Product;
import studio.aroundhub.tunagiftset.entity.type.OrderStatus;
import studio.aroundhub.tunagiftset.entity.type.PaymentStatus;
import studio.aroundhub.tunagiftset.entity.type.ProductStatus;
import studio.aroundhub.tunagiftset.order.dto.OrderCreateRequest;
import studio.aroundhub.tunagiftset.order.dto.OrderDetailResponse;
import studio.aroundhub.tunagiftset.order.service.OrderService;
import studio.aroundhub.tunagiftset.repository.BrandRepository;
import studio.aroundhub.tunagiftset.repository.CartItemRepository;
import studio.aroundhub.tunagiftset.repository.CartRepository;
import studio.aroundhub.tunagiftset.repository.CategoryRepository;
import studio.aroundhub.tunagiftset.repository.DeliveryRepository;
import studio.aroundhub.tunagiftset.repository.InventoryHistoryRepository;
import studio.aroundhub.tunagiftset.repository.MemberRepository;
import studio.aroundhub.tunagiftset.repository.OrderItemRepository;
import studio.aroundhub.tunagiftset.repository.OrderRepository;
import studio.aroundhub.tunagiftset.repository.PaymentRepository;
import studio.aroundhub.tunagiftset.repository.ProductImageRepository;
import studio.aroundhub.tunagiftset.repository.ProductRepository;
import studio.aroundhub.tunagiftset.security.JwtTokenProvider;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentConfirmIntegrationTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private OrderService orderService;

    @Autowired
    private FakePaymentGatewayClient fakePaymentGatewayClient;

    @Autowired
    private Clock clock;

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
                "payment-user@example.com",
                passwordEncoder.encode("SamplePassword123!"),
                "SAMPLE USER",
                "01012345678"
        ));
        anotherMember = memberRepository.save(new Member(
                "another-payment-user@example.com",
                passwordEncoder.encode("SamplePassword123!"),
                "ANOTHER SAMPLE USER",
                "01087654321"
        ));
        memberToken = jwtTokenProvider.createAccessToken(member);
        anotherToken = jwtTokenProvider.createAccessToken(anotherMember);
        brand = brandRepository.save(new Brand("PAYMENT-SAMPLE", "PAYMENT SAMPLE BRAND", true));
        category = categoryRepository.save(new Category(null, "PAYMENT SAMPLE CATEGORY", 1, true));
    }

    @Test
    void confirmSucceedsAndMarksOrderAndPaymentPaid() throws Exception {
        String orderNumber = createOrder(member, 45000, 2).orderNumber();

        HttpResponse<String> response = confirm(orderNumber, "pay_key_1", 90000, memberToken);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(response.body(), "$.orderStatus")).isEqualTo("PAID");
        assertThat(JsonPath.<String>read(response.body(), "$.paymentStatus")).isEqualTo("PAID");
        assertThat(JsonPath.<Integer>read(response.body(), "$.amount")).isEqualTo(90000);
        assertThat(JsonPath.<String>read(response.body(), "$.method")).isEqualTo("CARD");
        assertThat(JsonPath.<String>read(response.body(), "$.approvedAt")).isNotBlank();

        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow();
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPaymentKey()).isEqualTo("pay_key_1");
        assertThat(payment.getApprovedAmount()).isEqualByComparingTo(BigDecimal.valueOf(90000));
        assertThat(payment.getPaidAt()).isNotNull();
    }

    @Test
    void confirmRejectsAmountLowerThanOrderTotalWithoutCallingToss() throws Exception {
        String orderNumber = createOrder(member, 93000, 1).orderNumber();

        HttpResponse<String> response = confirm(orderNumber, "pay_key_2", 100, memberToken);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo("PAYMENT_AMOUNT_MISMATCH");
        assertThat(fakePaymentGatewayClient.confirmCallCount()).isZero();
        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
    }

    @Test
    void confirmRejectsAmountOffByOneWithoutCallingToss() throws Exception {
        String orderNumber = createOrder(member, 93000, 1).orderNumber();

        HttpResponse<String> response = confirm(orderNumber, "pay_key_3", 93001, memberToken);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo("PAYMENT_AMOUNT_MISMATCH");
        assertThat(fakePaymentGatewayClient.confirmCallCount()).isZero();
    }

    @Test
    void confirmRequiresAuthentication() throws Exception {
        String orderNumber = createOrder(member, 60000, 1).orderNumber();

        HttpResponse<String> response = confirm(orderNumber, "pay_key_4", 10000, null);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(fakePaymentGatewayClient.confirmCallCount()).isZero();
    }

    @Test
    void confirmBlocksOtherMembersOrder() throws Exception {
        String orderNumber = createOrder(member, 60000, 1).orderNumber();

        HttpResponse<String> response = confirm(orderNumber, "pay_key_5", 10000, anotherToken);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(fakePaymentGatewayClient.confirmCallCount()).isZero();
        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
    }

    @Test
    void confirmFailsForNonExistentOrder() throws Exception {
        HttpResponse<String> response = confirm("TG20260101-NOPE0000", "pay_key_6", 10000, memberToken);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo("ORDER_NOT_FOUND");
    }

    @Test
    void repeatingTheSameConfirmIsIdempotent() throws Exception {
        String orderNumber = createOrder(member, 60000, 1).orderNumber();

        HttpResponse<String> first = confirm(orderNumber, "pay_key_7", 60000, memberToken);
        HttpResponse<String> replay = confirm(orderNumber, "pay_key_7", 60000, memberToken);

        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(replay.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(replay.body(), "$.orderStatus")).isEqualTo("PAID");
        // The second call short-circuits on the already-PAID check before ever reaching Toss.
        assertThat(fakePaymentGatewayClient.confirmCallCount()).isEqualTo(1);
    }

    @Test
    void confirmingAnAlreadyPaidOrderWithADifferentPaymentKeyIsRejected() throws Exception {
        String orderNumber = createOrder(member, 60000, 1).orderNumber();
        confirm(orderNumber, "pay_key_8", 60000, memberToken);

        HttpResponse<String> response = confirm(orderNumber, "pay_key_9_different", 60000, memberToken);

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo("PAYMENT_ALREADY_COMPLETED");
        assertThat(fakePaymentGatewayClient.confirmCallCount()).isEqualTo(1);
    }

    @Test
    void reusingAPaymentKeyAlreadyBoundToAnotherOrderIsRejected() throws Exception {
        String orderNumberA = createOrder(member, 60000, 1).orderNumber();
        String orderNumberB = createOrder(member, 70000, 1).orderNumber();
        confirm(orderNumberA, "shared_pay_key", 60000, memberToken);

        HttpResponse<String> response = confirm(orderNumberB, "shared_pay_key", 70000, memberToken);

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo("PAYMENT_DUPLICATE_REQUEST");
    }

    @Test
    void confirmOnAnExpiredOrderIsRejectedEvenBeforeTheSchedulerRuns() throws Exception {
        String orderNumber = createOrder(member, 60000, 1).orderNumber();
        setExpiresAtInPast(orderNumber);

        HttpResponse<String> response = confirm(orderNumber, "pay_key_expired", 60000, memberToken);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo("ORDER_EXPIRED");
        assertThat(fakePaymentGatewayClient.confirmCallCount()).isZero();
    }

    @Test
    void tossRejectionKeepsOrderPendingAndMarksPaymentFailed() throws Exception {
        String orderNumber = createOrder(member, 60000, 1).orderNumber();
        fakePaymentGatewayClient.queueConfirmFailure("REJECT_CARD_COMPANY", false);

        HttpResponse<String> response = confirm(orderNumber, "pay_key_fail", 60000, memberToken);

        assertThat(response.statusCode()).isEqualTo(502);
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo("PAYMENT_PROVIDER_ERROR");
        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow();
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void tossTimeoutReturnsGatewayTimeoutAndKeepsOrderPending() throws Exception {
        String orderNumber = createOrder(member, 60000, 1).orderNumber();
        fakePaymentGatewayClient.queueConfirmFailure("PROVIDER_TIMEOUT", true);

        HttpResponse<String> response = confirm(orderNumber, "pay_key_timeout", 60000, memberToken);

        assertThat(response.statusCode()).isEqualTo(504);
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo("PAYMENT_PROVIDER_TIMEOUT");
        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
    }

    @Test
    void aFailedConfirmCanBeRetriedSuccessfully() throws Exception {
        String orderNumber = createOrder(member, 60000, 1).orderNumber();
        fakePaymentGatewayClient.queueConfirmFailure("TEMPORARY_ERROR", false);
        confirm(orderNumber, "pay_key_retry_1", 60000, memberToken);

        HttpResponse<String> retry = confirm(orderNumber, "pay_key_retry_2", 60000, memberToken);

        assertThat(retry.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(retry.body(), "$.orderStatus")).isEqualTo("PAID");
    }

    @Test
    void concurrentConfirmCallsResultInExactlyOnePaidOutcome() throws Exception {
        String orderNumber = createOrder(member, 60000, 1).orderNumber();
        int attempts = 8;
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        List<Callable<Void>> tasks = new java.util.ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            tasks.add(() -> {
                startLatch.await(5, TimeUnit.SECONDS);
                HttpResponse<String> response = confirm(orderNumber, "pay_key_race", 60000, memberToken);
                if (response.statusCode() == 200) {
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

        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow();
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getApprovedAmount()).isEqualByComparingTo(BigDecimal.valueOf(60000));
        assertThat(successCount.get()).isGreaterThanOrEqualTo(1);
    }

    private OrderDetailResponse createOrder(Member owner, int unitPrice, int quantity) {
        Product product = productRepository.save(new Product(
                brand,
                category,
                "PAYMENT-SAMPLE-" + System.nanoTime(),
                "SAMPLE PAYMENT PRODUCT",
                "SAMPLE",
                "SAMPLE PAYMENT PRODUCT DATA",
                BigDecimal.valueOf(unitPrice),
                BigDecimal.valueOf(unitPrice),
                10,
                ProductStatus.ACTIVE,
                false
        ));
        Cart cart = cartRepository.findByMemberId(owner.getId())
                .orElseGet(() -> cartRepository.save(new Cart(owner)));
        CartItem cartItem = cartItemRepository.save(new CartItem(cart, product, quantity));

        return orderService.createOrder(owner.getId(), new OrderCreateRequest(
                List.of(cartItem.getId()),
                "SAMPLE RECIPIENT",
                "01012345678",
                "12345",
                "SEOUL SAMPLE ADDRESS",
                "101",
                "LEAVE AT DOOR"
        ));
    }

    private void setExpiresAtInPast(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow();
        ReflectionTestUtils.setField(order, "expiresAt", clock.instant().minusSeconds(60));
        orderRepository.save(order);
    }

    private HttpResponse<String> confirm(String orderId, String paymentKey, int amount, String token) throws Exception {
        String json = """
                {
                  "paymentKey": "%s",
                  "orderId": "%s",
                  "amount": %d
                }
                """.formatted(paymentKey, orderId, amount);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/payments/confirm"))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(json));
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
