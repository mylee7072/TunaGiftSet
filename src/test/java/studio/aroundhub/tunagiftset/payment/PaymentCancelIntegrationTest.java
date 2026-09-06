package studio.aroundhub.tunagiftset.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
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
class PaymentCancelIntegrationTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private OrderService orderService;

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Member member;
    private String memberToken;
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
                "cancel-user@example.com",
                passwordEncoder.encode("SamplePassword123!"),
                "SAMPLE USER",
                "01012345678"
        ));
        memberToken = jwtTokenProvider.createAccessToken(member);
        brand = brandRepository.save(new Brand("CANCEL-SAMPLE", "CANCEL SAMPLE BRAND", true));
        category = categoryRepository.save(new Category(null, "CANCEL SAMPLE CATEGORY", 1, true));
    }

    @Test
    void cancelingAPaidOrderCallsTossAndRestoresStock() throws Exception {
        Product product = saveProduct(10, BigDecimal.valueOf(20000));
        String orderNumber = createOrder(product, 3).orderNumber();
        confirm(orderNumber, "paid_cancel_key", 60000);

        HttpResponse<String> response = cancel(orderNumber, memberToken);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(response.body(), "$.orderStatus")).isEqualTo("CANCELED");
        assertThat(fakePaymentGatewayClient.cancelCallCount()).isEqualTo(1);

        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow();
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(payment.getCanceledAt()).isNotNull();
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(10);
    }

    @Test
    void duplicateCancelOfAPaidOrderIsIdempotentAndDoesNotDoubleRestoreStock() throws Exception {
        Product product = saveProduct(10, BigDecimal.valueOf(30000));
        String orderNumber = createOrder(product, 2).orderNumber();
        confirm(orderNumber, "paid_cancel_dup_key", 60000);

        HttpResponse<String> first = cancel(orderNumber, memberToken);
        HttpResponse<String> duplicate = cancel(orderNumber, memberToken);

        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(duplicate.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(duplicate.body(), "$.orderStatus")).isEqualTo("CANCELED");
        assertThat(fakePaymentGatewayClient.cancelCallCount()).isEqualTo(1);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(10);
    }

    @Test
    void tossCancelFailureLeavesOrderAndPaymentPaid() throws Exception {
        Product product = saveProduct(10, BigDecimal.valueOf(15000));
        String orderNumber = createOrder(product, 4).orderNumber();
        confirm(orderNumber, "paid_cancel_fail_key", 60000);
        fakePaymentGatewayClient.queueCancelFailure("CANCEL_REJECTED", false);

        HttpResponse<String> response = cancel(orderNumber, memberToken);

        assertThat(response.statusCode()).isEqualTo(502);
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo("PAYMENT_PROVIDER_ERROR");

        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow();
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(6);
    }

    @Test
    void unpaidOrderCancelDoesNotCallToss() throws Exception {
        Product product = saveProduct(10, BigDecimal.valueOf(10000));
        String orderNumber = createOrder(product, 1).orderNumber();

        HttpResponse<String> response = cancel(orderNumber, memberToken);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(fakePaymentGatewayClient.cancelCallCount()).isZero();
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(10);
    }

    @Test
    void concurrentCancelOfAPaidOrderEndsInASingleConsistentCanceledState() throws Exception {
        Product product = saveProduct(20, BigDecimal.valueOf(10000));
        String orderNumber = createOrder(product, 5).orderNumber();
        confirm(orderNumber, "paid_cancel_race_key", 50000);

        int attempts = 6;
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Callable<Integer>> tasks = new java.util.ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            tasks.add(() -> {
                startLatch.await(5, TimeUnit.SECONDS);
                return cancel(orderNumber, memberToken).statusCode();
            });
        }

        var executor = Executors.newFixedThreadPool(attempts);
        var futures = tasks.stream().map(executor::submit).toList();
        startLatch.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        for (var future : futures) {
            assertThat(future.get()).isEqualTo(200);
        }

        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow();
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        // Stock must be restored exactly once regardless of how many racing cancel calls landed.
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(20);
    }

    private Product saveProduct(int stockQuantity, BigDecimal salePrice) {
        return productRepository.save(new Product(
                brand,
                category,
                "CANCEL-SAMPLE-" + System.nanoTime(),
                "SAMPLE CANCEL PRODUCT",
                "SAMPLE",
                "SAMPLE CANCEL PRODUCT DATA",
                salePrice,
                salePrice,
                stockQuantity,
                ProductStatus.ACTIVE,
                false
        ));
    }

    private OrderDetailResponse createOrder(Product product, int quantity) {
        Cart cart = cartRepository.findByMemberId(member.getId())
                .orElseGet(() -> cartRepository.save(new Cart(member)));
        CartItem cartItem = cartItemRepository.save(new CartItem(cart, product, quantity));
        return orderService.createOrder(member.getId(), new OrderCreateRequest(
                List.of(cartItem.getId()),
                "SAMPLE RECIPIENT",
                "01012345678",
                "12345",
                "SEOUL SAMPLE ADDRESS",
                "101",
                "LEAVE AT DOOR"
        ));
    }

    private void confirm(String orderId, String paymentKey, int amount) throws Exception {
        String json = """
                {
                  "paymentKey": "%s",
                  "orderId": "%s",
                  "amount": %d
                }
                """.formatted(paymentKey, orderId, amount);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/payments/confirm"))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
    }

    private HttpResponse<String> cancel(String orderNumber, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/orders/" + orderNumber + "/cancel"))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString("{}"));
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
