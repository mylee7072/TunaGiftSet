package studio.aroundhub.tunagiftset.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
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
import studio.aroundhub.tunagiftset.entity.Product;
import studio.aroundhub.tunagiftset.entity.type.OrderStatus;
import studio.aroundhub.tunagiftset.entity.type.ProductStatus;
import studio.aroundhub.tunagiftset.exception.InvalidRequestException;
import studio.aroundhub.tunagiftset.order.dto.OrderCreateRequest;
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
class OrderIntegrationTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private OrderService orderService;

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
                "order-user@example.com",
                passwordEncoder.encode("SamplePassword123!"),
                "SAMPLE USER",
                "01012345678"
        ));
        anotherMember = memberRepository.save(new Member(
                "another-order-user@example.com",
                passwordEncoder.encode("SamplePassword123!"),
                "ANOTHER SAMPLE USER",
                "01087654321"
        ));
        memberToken = jwtTokenProvider.createAccessToken(member);
        anotherToken = jwtTokenProvider.createAccessToken(anotherMember);
        brand = brandRepository.save(new Brand("ORDER-SAMPLE", "ORDER SAMPLE BRAND", true));
        category = categoryRepository.save(new Category(null, "ORDER SAMPLE CATEGORY", 1, true));
    }

    @Test
    void createOrderWithSingleCartItem() throws Exception {
        Product product = saveProduct("ORDER-SAMPLE-001", ProductStatus.ACTIVE, 10, BigDecimal.valueOf(45000));
        CartItem cartItem = addCartItem(member, product, 2);

        HttpResponse<String> response = post("/api/orders", orderJson(List.of(cartItem.getId())), memberToken);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(response.body(), "$.orderNumber")).startsWith("TG");
        assertThat(JsonPath.<String>read(response.body(), "$.orderStatus")).isEqualTo("PAYMENT_PENDING");
        assertThat(JsonPath.<String>read(response.body(), "$.items[0].productName")).isEqualTo("SAMPLE ORDER PRODUCT");
        assertThat(JsonPath.<Integer>read(response.body(), "$.items[0].unitPrice")).isEqualTo(45000);
        assertThat(JsonPath.<Integer>read(response.body(), "$.items[0].quantity")).isEqualTo(2);
        assertThat(JsonPath.<Integer>read(response.body(), "$.items[0].totalPrice")).isEqualTo(90000);
        assertThat(JsonPath.<Integer>read(response.body(), "$.productAmount")).isEqualTo(90000);
        assertThat(JsonPath.<Integer>read(response.body(), "$.shippingFee")).isZero();
        assertThat(JsonPath.<Integer>read(response.body(), "$.totalAmount")).isEqualTo(90000);
        assertThat(JsonPath.<String>read(response.body(), "$.recipientName")).isEqualTo("SAMPLE RECIPIENT");
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(8);
        assertThat(cartItemRepository.findById(cartItem.getId())).isEmpty();
        assertThat(paymentRepository.findByOrderId(orderRepository.findAll().get(0).getId())).isPresent();
        assertThat(deliveryRepository.findByOrderId(orderRepository.findAll().get(0).getId())).isPresent();
    }

    @Test
    void createOrderWithMultipleItemsAndSelectedItemsOnly() throws Exception {
        Product product1 = saveProduct("ORDER-SAMPLE-002", ProductStatus.ACTIVE, 10, BigDecimal.valueOf(10000));
        Product product2 = saveProduct("ORDER-SAMPLE-003", ProductStatus.ACTIVE, 10, BigDecimal.valueOf(20000));
        Product product3 = saveProduct("ORDER-SAMPLE-004", ProductStatus.ACTIVE, 10, BigDecimal.valueOf(30000));
        CartItem item1 = addCartItem(member, product1, 1);
        CartItem item2 = addCartItem(member, product2, 2);
        CartItem item3 = addCartItem(member, product3, 1);

        HttpResponse<String> response = post("/api/orders", orderJson(List.of(item1.getId(), item3.getId())), memberToken);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Integer>read(response.body(), "$.items.length()")).isEqualTo(2);
        assertThat(JsonPath.<Integer>read(response.body(), "$.productAmount")).isEqualTo(40000);
        assertThat(JsonPath.<Integer>read(response.body(), "$.shippingFee")).isEqualTo(3000);
        assertThat(JsonPath.<Integer>read(response.body(), "$.totalAmount")).isEqualTo(43000);
        assertThat(cartItemRepository.findById(item1.getId())).isEmpty();
        assertThat(cartItemRepository.findById(item2.getId())).isPresent();
        assertThat(cartItemRepository.findById(item3.getId())).isEmpty();
    }

    @Test
    void orderItemKeepsSnapshotAfterProductChanged() throws Exception {
        Product product = saveProduct("ORDER-SAMPLE-005", ProductStatus.ACTIVE, 10, BigDecimal.valueOf(45000));
        CartItem cartItem = addCartItem(member, product, 1);
        String orderNumber = JsonPath.read(post("/api/orders", orderJson(List.of(cartItem.getId())), memberToken).body(), "$.orderNumber");

        product = productRepository.findById(product.getId()).orElseThrow();
        product.update(brand, category, "ORDER-SAMPLE-005", "UPDATED PRODUCT", "SAMPLE", "SAMPLE", BigDecimal.valueOf(50000), BigDecimal.valueOf(50000), 9, ProductStatus.ACTIVE, false);
        productRepository.save(product);

        HttpResponse<String> detail = get("/api/orders/" + orderNumber, memberToken);

        assertThat(detail.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(detail.body(), "$.items[0].productName")).isEqualTo("SAMPLE ORDER PRODUCT");
        assertThat(JsonPath.<Integer>read(detail.body(), "$.items[0].unitPrice")).isEqualTo(45000);
    }

    @Test
    void createOrderFailsWhenStockInsufficientAndRollsBack() throws Exception {
        Product product = saveProduct("ORDER-SAMPLE-006", ProductStatus.ACTIVE, 2, BigDecimal.valueOf(10000));
        CartItem cartItem = addCartItem(member, product, 3);

        HttpResponse<String> response = post("/api/orders", orderJson(List.of(cartItem.getId())), memberToken);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo("INSUFFICIENT_STOCK");
        assertThat(orderRepository.findAll()).isEmpty();
        assertThat(cartItemRepository.findById(cartItem.getId())).isPresent();
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(2);
    }

    @Test
    void createOrderFailsWhenProductIsNotAvailable() throws Exception {
        Product soldOut = saveProduct("ORDER-SAMPLE-007", ProductStatus.SOLD_OUT, 10, BigDecimal.valueOf(10000));
        Product hidden = saveProduct("ORDER-SAMPLE-008", ProductStatus.HIDDEN, 10, BigDecimal.valueOf(10000));
        Product discontinued = saveProduct("ORDER-SAMPLE-009", ProductStatus.DISCONTINUED, 10, BigDecimal.valueOf(10000));
        Product zeroStock = saveProduct("ORDER-SAMPLE-010", ProductStatus.ACTIVE, 0, BigDecimal.valueOf(10000));

        assertThat(post("/api/orders", orderJson(List.of(addCartItem(member, soldOut, 1).getId())), memberToken).statusCode()).isEqualTo(400);
        assertThat(post("/api/orders", orderJson(List.of(addCartItem(member, hidden, 1).getId())), memberToken).statusCode()).isEqualTo(400);
        assertThat(post("/api/orders", orderJson(List.of(addCartItem(member, discontinued, 1).getId())), memberToken).statusCode()).isEqualTo(400);
        assertThat(post("/api/orders", orderJson(List.of(addCartItem(member, zeroStock, 1).getId())), memberToken).statusCode()).isEqualTo(400);
    }

    @Test
    void createOrderValidatesCartItemIdsAndAddress() throws Exception {
        Product product = saveProduct("ORDER-SAMPLE-011", ProductStatus.ACTIVE, 10, BigDecimal.valueOf(10000));
        CartItem cartItem = addCartItem(member, product, 1);
        CartItem otherItem = addCartItem(anotherMember, product, 1);

        assertThat(post("/api/orders", orderJson(List.of()), memberToken).statusCode()).isEqualTo(400);
        assertThat(post("/api/orders", orderJson(List.of(cartItem.getId(), cartItem.getId())), memberToken).statusCode()).isEqualTo(400);
        assertThat(post("/api/orders", orderJson(List.of(999999L)), memberToken).statusCode()).isEqualTo(404);
        assertThat(post("/api/orders", orderJson(List.of(otherItem.getId())), memberToken).statusCode()).isEqualTo(404);

        HttpResponse<String> invalidAddress = post("/api/orders", """
                {
                  "cartItemIds": [%d],
                  "recipientName": "",
                  "recipientPhone": "",
                  "zipCode": "",
                  "address1": "",
                  "address2": "101",
                  "deliveryMessage": "SAMPLE"
                }
                """.formatted(cartItem.getId()), memberToken);
        assertThat(invalidAddress.statusCode()).isEqualTo(400);
    }

    @Test
    void orderApisRequireAuthenticationAndProtectOwnership() throws Exception {
        Product product = saveProduct("ORDER-SAMPLE-012", ProductStatus.ACTIVE, 10, BigDecimal.valueOf(10000));
        CartItem cartItem = addCartItem(member, product, 1);
        String orderNumber = JsonPath.read(post("/api/orders", orderJson(List.of(cartItem.getId())), memberToken).body(), "$.orderNumber");

        assertThat(post("/api/orders", orderJson(List.of(cartItem.getId())), null).statusCode()).isEqualTo(401);
        assertThat(get("/api/orders", null).statusCode()).isEqualTo(401);
        assertThat(get("/api/orders/" + orderNumber, anotherToken).statusCode()).isEqualTo(404);
        assertThat(post("/api/orders/" + orderNumber + "/cancel", "{}", anotherToken).statusCode()).isEqualTo(404);
        assertThat(get("/api/orders/" + orderNumber, memberToken).statusCode()).isEqualTo(200);
    }

    @Test
    void findOrdersAndDetail() throws Exception {
        Product product1 = saveProduct("ORDER-SAMPLE-013", ProductStatus.ACTIVE, 10, BigDecimal.valueOf(10000));
        Product product2 = saveProduct("ORDER-SAMPLE-014", ProductStatus.ACTIVE, 10, BigDecimal.valueOf(20000));
        CartItem item1 = addCartItem(member, product1, 1);
        CartItem item2 = addCartItem(member, product2, 1);
        String orderNumber = JsonPath.read(post("/api/orders", orderJson(List.of(item1.getId(), item2.getId())), memberToken).body(), "$.orderNumber");

        HttpResponse<String> list = get("/api/orders", memberToken);
        HttpResponse<String> detail = get("/api/orders/" + orderNumber, memberToken);

        assertThat(list.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Integer>read(list.body(), "$.content.length()")).isEqualTo(1);
        assertThat(JsonPath.<String>read(list.body(), "$.content[0].representativeProductName")).isEqualTo("SAMPLE ORDER PRODUCT 외 1건");
        assertThat(detail.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(detail.body(), "$.payment.status")).isEqualTo("READY");
        assertThat(JsonPath.<String>read(detail.body(), "$.delivery.status")).isEqualTo("READY");
    }

    @Test
    void cancelOrderRestoresStockAndPreventsDuplicateCancel() throws Exception {
        Product product = saveProduct("ORDER-SAMPLE-015", ProductStatus.ACTIVE, 10, BigDecimal.valueOf(10000));
        CartItem cartItem = addCartItem(member, product, 3);
        String orderNumber = JsonPath.read(post("/api/orders", orderJson(List.of(cartItem.getId())), memberToken).body(), "$.orderNumber");

        HttpResponse<String> cancelResponse = post("/api/orders/" + orderNumber + "/cancel", "{}", memberToken);
        HttpResponse<String> duplicateResponse = post("/api/orders/" + orderNumber + "/cancel", "{}", memberToken);

        assertThat(cancelResponse.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(cancelResponse.body(), "$.orderStatus")).isEqualTo("CANCELED");
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(10);
        assertThat(duplicateResponse.statusCode()).isEqualTo(400);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(10);
    }

    @Test
    void cancelOrderFailsWhenShippingOrDelivered() {
        Product shippingProduct = saveProduct("ORDER-SAMPLE-016", ProductStatus.ACTIVE, 10, BigDecimal.valueOf(10000));
        Product deliveredProduct = saveProduct("ORDER-SAMPLE-017", ProductStatus.ACTIVE, 10, BigDecimal.valueOf(10000));
        String shippingOrderNumber = createOrderNumber(member, shippingProduct, OrderStatus.SHIPPING);
        String deliveredOrderNumber = createOrderNumber(member, deliveredProduct, OrderStatus.DELIVERED);

        assertThatThrownBy(() -> orderService.cancel(member.getId(), shippingOrderNumber))
                .isInstanceOfSatisfying(InvalidRequestException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("ORDER_CANNOT_BE_CANCELED"));
        assertThatThrownBy(() -> orderService.cancel(member.getId(), deliveredOrderNumber))
                .isInstanceOfSatisfying(InvalidRequestException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("ORDER_CANNOT_BE_CANCELED"));
    }

    @Test
    void concurrentOrdersDoNotOversellStock() throws Exception {
        Product product = saveProduct("ORDER-SAMPLE-018", ProductStatus.ACTIVE, 5, BigDecimal.valueOf(10000));
        List<Member> members = new ArrayList<>();
        List<CartItem> cartItems = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Member user = memberRepository.save(new Member(
                    "concurrent-" + i + "@example.com",
                    passwordEncoder.encode("SamplePassword123!"),
                    "SAMPLE USER " + i,
                    "01012345%04d".formatted(i)
            ));
            members.add(user);
            cartItems.add(addCartItem(user, product, 1));
        }

        CountDownLatch startLatch = new CountDownLatch(1);
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            int index = i;
            tasks.add(() -> {
                startLatch.await(5, TimeUnit.SECONDS);
                try {
                    orderService.createOrder(
                            members.get(index).getId(),
                            new OrderCreateRequest(
                                    List.of(cartItems.get(index).getId()),
                                    "SAMPLE RECIPIENT",
                                    "01012345678",
                                    "12345",
                                    "SEOUL SAMPLE ADDRESS",
                                    "101",
                                    "LEAVE AT DOOR"
                            )
                    );
                    return true;
                } catch (RuntimeException exception) {
                    return false;
                }
            });
        }

        var executor = Executors.newFixedThreadPool(10);
        var futures = tasks.stream().map(executor::submit).toList();
        startLatch.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        long successCount = 0;
        for (var future : futures) {
            if (future.get()) {
                successCount++;
            }
        }

        assertThat(successCount).isLessThanOrEqualTo(5);
        assertThat(orderRepository.findAll()).hasSize((int) successCount);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(5 - (int) successCount);
    }

    private String createOrderNumber(Member owner, Product product, OrderStatus status) {
        CartItem cartItem = addCartItem(owner, product, 1);
        String orderNumber = orderService.createOrder(owner.getId(), new OrderCreateRequest(
                List.of(cartItem.getId()),
                "SAMPLE RECIPIENT",
                "01012345678",
                "12345",
                "SEOUL SAMPLE ADDRESS",
                "101",
                "LEAVE AT DOOR"
        )).orderNumber();
        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow();
        order.changeStatus(status);
        orderRepository.save(order);
        return orderNumber;
    }

    private Product saveProduct(String code, ProductStatus status, int stockQuantity, BigDecimal salePrice) {
        return productRepository.save(new Product(
                brand,
                category,
                code,
                "SAMPLE ORDER PRODUCT",
                "SAMPLE",
                "SAMPLE ORDER PRODUCT DATA",
                salePrice,
                salePrice,
                stockQuantity,
                status,
                false
        ));
    }

    private CartItem addCartItem(Member owner, Product product, int quantity) {
        Cart cart = cartRepository.findByMemberId(owner.getId())
                .orElseGet(() -> cartRepository.save(new Cart(owner)));
        return cartItemRepository.save(new CartItem(cart, product, quantity));
    }

    private String orderJson(List<Long> cartItemIds) {
        return """
                {
                  "cartItemIds": %s,
                  "recipientName": "SAMPLE RECIPIENT",
                  "recipientPhone": "01012345678",
                  "zipCode": "12345",
                  "address1": "SEOUL SAMPLE ADDRESS",
                  "address2": "101",
                  "deliveryMessage": "LEAVE AT DOOR"
                }
                """.formatted(cartItemIds);
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri(path))
                .GET();
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
