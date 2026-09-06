package studio.aroundhub.tunagiftset.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
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
import studio.aroundhub.tunagiftset.entity.Delivery;
import studio.aroundhub.tunagiftset.entity.Member;
import studio.aroundhub.tunagiftset.entity.Order;
import studio.aroundhub.tunagiftset.entity.OrderItem;
import studio.aroundhub.tunagiftset.entity.Product;
import studio.aroundhub.tunagiftset.entity.Review;
import studio.aroundhub.tunagiftset.entity.type.DeliveryStatus;
import studio.aroundhub.tunagiftset.entity.type.MemberRole;
import studio.aroundhub.tunagiftset.entity.type.MemberStatus;
import studio.aroundhub.tunagiftset.entity.type.OrderStatus;
import studio.aroundhub.tunagiftset.entity.type.ProductStatus;
import studio.aroundhub.tunagiftset.entity.type.ReviewStatus;
import studio.aroundhub.tunagiftset.order.dto.OrderCreateRequest;
import studio.aroundhub.tunagiftset.order.service.OrderService;
import studio.aroundhub.tunagiftset.repository.AddressRepository;
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
import studio.aroundhub.tunagiftset.repository.ProductQuestionAnswerRepository;
import studio.aroundhub.tunagiftset.repository.ProductQuestionRepository;
import studio.aroundhub.tunagiftset.repository.ProductRepository;
import studio.aroundhub.tunagiftset.repository.ReviewRepository;
import studio.aroundhub.tunagiftset.security.JwtTokenProvider;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReviewQuestionIntegrationTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductQuestionRepository questionRepository;

    @Autowired
    private ProductQuestionAnswerRepository answerRepository;

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
    private AddressRepository addressRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Member member;
    private Member otherMember;
    private Member admin;
    private String memberToken;
    private String otherToken;
    private String adminToken;
    private Brand brand;
    private Category category;

    @BeforeEach
    void setUp() {
        clean();
        member = memberRepository.save(new Member("review-user@example.com", passwordEncoder.encode("SamplePassword123!"), "SAMPLE USER", "01012345678"));
        otherMember = memberRepository.save(new Member("review-other@example.com", passwordEncoder.encode("SamplePassword123!"), "OTHER USER", "01087654321"));
        admin = memberRepository.save(new Member("review-admin@example.com", passwordEncoder.encode("SamplePassword123!"), "ADMIN USER", "01011112222", MemberRole.ADMIN, MemberStatus.ACTIVE));
        memberToken = jwtTokenProvider.createAccessToken(member);
        otherToken = jwtTokenProvider.createAccessToken(otherMember);
        adminToken = jwtTokenProvider.createAccessToken(admin);
        brand = brandRepository.save(new Brand("REVIEW-BRAND", "REVIEW BRAND", true));
        category = categoryRepository.save(new Category(null, "REVIEW CATEGORY", 1, true));
    }

    @AfterEach
    void tearDown() {
        clean();
    }

    @Test
    void reviewRequiresDeliveredOwnedOrderItemAndValidProduct() throws Exception {
        Product product = saveProduct("REVIEW-PRODUCT-1");
        Product otherProduct = saveProduct("REVIEW-PRODUCT-2");
        OrderItem pendingItem = createOrderItem(member, product, OrderStatus.PAYMENT_PENDING, DeliveryStatus.READY);
        OrderItem deliveredItem = createOrderItem(member, product, OrderStatus.DELIVERED, DeliveryStatus.DELIVERED);
        OrderItem otherMemberItem = createOrderItem(otherMember, product, OrderStatus.DELIVERED, DeliveryStatus.DELIVERED);
        OrderItem otherProductItem = createOrderItem(member, otherProduct, OrderStatus.DELIVERED, DeliveryStatus.DELIVERED);

        assertThat(post("/api/products/" + product.getId() + "/reviews", reviewJson(deliveredItem.getId(), 5, "This gift set was excellent."), null).statusCode()).isEqualTo(401);
        assertThat(post("/api/products/" + product.getId() + "/reviews", reviewJson(pendingItem.getId(), 5, "This gift set was excellent."), memberToken).statusCode()).isEqualTo(400);
        assertThat(post("/api/products/" + product.getId() + "/reviews", reviewJson(otherMemberItem.getId(), 5, "This gift set was excellent."), memberToken).statusCode()).isEqualTo(404);
        assertThat(post("/api/products/" + product.getId() + "/reviews", reviewJson(otherProductItem.getId(), 5, "This gift set was excellent."), memberToken).statusCode()).isEqualTo(400);

        HttpResponse<String> created = post("/api/products/" + product.getId() + "/reviews", reviewJson(deliveredItem.getId(), 5, "This gift set was excellent."), memberToken);
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(JsonPath.<Boolean>read(created.body(), "$.verifiedPurchase")).isTrue();
    }

    @Test
    void reviewValidationDuplicatePermissionAndAdminVisibility() throws Exception {
        Product product = saveProduct("REVIEW-PRODUCT-3");
        OrderItem orderItem = createOrderItem(member, product, OrderStatus.DELIVERED, DeliveryStatus.DELIVERED);

        assertThat(post("/api/products/" + product.getId() + "/reviews", reviewJson(orderItem.getId(), 0, "This gift set was excellent."), memberToken).statusCode()).isEqualTo(400);
        assertThat(post("/api/products/" + product.getId() + "/reviews", reviewJson(orderItem.getId(), 6, "This gift set was excellent."), memberToken).statusCode()).isEqualTo(400);
        assertThat(post("/api/products/" + product.getId() + "/reviews", reviewJson(orderItem.getId(), 5, "short"), memberToken).statusCode()).isEqualTo(400);

        Long reviewId = JsonPath.<Number>read(post("/api/products/" + product.getId() + "/reviews", reviewJson(orderItem.getId(), 5, "This gift set was excellent."), memberToken).body(), "$.id").longValue();
        assertThat(post("/api/products/" + product.getId() + "/reviews", reviewJson(orderItem.getId(), 4, "Second review should fail."), memberToken).statusCode()).isEqualTo(400);

        assertThat(put("/api/reviews/" + reviewId, reviewUpdateJson(4, "Updated review content."), otherToken).statusCode()).isEqualTo(404);
        assertThat(put("/api/reviews/" + reviewId, reviewUpdateJson(4, "Updated review content."), memberToken).statusCode()).isEqualTo(200);
        assertThat(patch("/api/admin/reviews/" + reviewId + "/hide", "{}", memberToken).statusCode()).isEqualTo(403);
        assertThat(patch("/api/admin/reviews/" + reviewId + "/hide", "{}", adminToken).statusCode()).isEqualTo(200);

        HttpResponse<String> publicList = get("/api/products/" + product.getId() + "/reviews", null);
        assertThat(JsonPath.<Integer>read(publicList.body(), "$.content.length()")).isZero();
        assertThat(JsonPath.<Integer>read(get("/api/admin/reviews?status=HIDDEN", adminToken).body(), "$.content.length()")).isEqualTo(1);

        assertThat(patch("/api/admin/reviews/" + reviewId + "/show", "{}", adminToken).statusCode()).isEqualTo(200);
        assertThat(delete("/api/reviews/" + reviewId, otherToken).statusCode()).isEqualTo(404);
        assertThat(delete("/api/reviews/" + reviewId, memberToken).statusCode()).isEqualTo(204);
        assertThat(reviewRepository.findById(reviewId).orElseThrow().getStatus()).isEqualTo(ReviewStatus.DELETED);
    }

    @Test
    void duplicateReviewIsBlockedWhenRequestsRace() throws Exception {
        Product product = saveProduct("REVIEW-PRODUCT-4");
        OrderItem orderItem = createOrderItem(member, product, OrderStatus.DELIVERED, DeliveryStatus.DELIVERED);

        CountDownLatch startLatch = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        var first = executor.submit(() -> {
            startLatch.await(5, TimeUnit.SECONDS);
            return post("/api/products/" + product.getId() + "/reviews", reviewJson(orderItem.getId(), 5, "Concurrent review content one."), memberToken).statusCode();
        });
        var second = executor.submit(() -> {
            startLatch.await(5, TimeUnit.SECONDS);
            return post("/api/products/" + product.getId() + "/reviews", reviewJson(orderItem.getId(), 4, "Concurrent review content two."), memberToken).statusCode();
        });
        startLatch.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(List.of(first.get(), second.get())).contains(201);
        assertThat(reviewRepository.findAll()).hasSize(1);
    }

    @Test
    void ratingAggregateExcludesHiddenAndDeletedReviews() throws Exception {
        Product product = saveProduct("REVIEW-PRODUCT-5");
        Long firstReviewId = JsonPath.<Number>read(post("/api/products/" + product.getId() + "/reviews", reviewJson(createOrderItem(member, product, OrderStatus.DELIVERED, DeliveryStatus.DELIVERED).getId(), 5, "First visible review content."), memberToken).body(), "$.id").longValue();
        Long secondReviewId = JsonPath.<Number>read(post("/api/products/" + product.getId() + "/reviews", reviewJson(createOrderItem(otherMember, product, OrderStatus.DELIVERED, DeliveryStatus.DELIVERED).getId(), 1, "Second visible review content."), otherToken).body(), "$.id").longValue();

        HttpResponse<String> detail = get("/api/products/" + product.getId(), null);
        assertThat(JsonPath.<Number>read(detail.body(), "$.averageRating").doubleValue()).isEqualTo(3.0);
        assertThat(JsonPath.<Integer>read(detail.body(), "$.reviewCount")).isEqualTo(2);

        patch("/api/admin/reviews/" + secondReviewId + "/hide", "{}", adminToken);
        detail = get("/api/products/" + product.getId(), null);
        assertThat(JsonPath.<Number>read(detail.body(), "$.averageRating").doubleValue()).isEqualTo(5.0);
        assertThat(JsonPath.<Integer>read(detail.body(), "$.reviewCount")).isEqualTo(1);

        delete("/api/reviews/" + firstReviewId, memberToken);
        detail = get("/api/products/" + product.getId(), null);
        assertThat(JsonPath.<Integer>read(detail.body(), "$.reviewCount")).isZero();
    }

    @Test
    void questionSecretMaskingOwnerUpdateDeleteAndAdminAnswer() throws Exception {
        Product product = saveProduct("QUESTION-PRODUCT-1");

        assertThat(post("/api/products/" + product.getId() + "/questions", questionJson("Shipping", "Can I choose delivery date?", false), null).statusCode()).isEqualTo(401);
        assertThat(post("/api/products/" + product.getId() + "/questions", questionJson("", "", false), memberToken).statusCode()).isEqualTo(400);

        Long secretQuestionId = JsonPath.<Number>read(post("/api/products/" + product.getId() + "/questions", questionJson("Private question", "Please answer privately.", true), memberToken).body(), "$.id").longValue();
        HttpResponse<String> anonymousDetail = get("/api/questions/" + secretQuestionId, null);
        assertThat(JsonPath.<String>read(anonymousDetail.body(), "$.title")).isEqualTo("Secret question");
        assertThat(anonymousDetail.body()).doesNotContain("Please answer privately.");

        HttpResponse<String> otherDetail = get("/api/questions/" + secretQuestionId, otherToken);
        assertThat(otherDetail.body()).doesNotContain("Please answer privately.");
        assertThat(JsonPath.<String>read(get("/api/questions/" + secretQuestionId, memberToken).body(), "$.content")).isEqualTo("Please answer privately.");

        assertThat(put("/api/questions/" + secretQuestionId, questionJson("Attack", "Other user edit.", false), otherToken).statusCode()).isEqualTo(404);
        assertThat(put("/api/questions/" + secretQuestionId, questionJson("Updated", "Updated private question.", true), memberToken).statusCode()).isEqualTo(200);

        assertThat(post("/api/admin/questions/" + secretQuestionId + "/answer", answerJson("Seller answer content."), memberToken).statusCode()).isEqualTo(403);
        HttpResponse<String> answered = post("/api/admin/questions/" + secretQuestionId + "/answer", answerJson("Seller answer content."), adminToken);
        assertThat(answered.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(answered.body(), "$.status")).isEqualTo("ANSWERED");
        assertThat(JsonPath.<String>read(answered.body(), "$.answer.content")).isEqualTo("Seller answer content.");

        assertThat(get("/api/questions/" + secretQuestionId, otherToken).body()).doesNotContain("Seller answer content.");
        assertThat(put("/api/questions/" + secretQuestionId, questionJson("Cannot edit", "Answered question edit.", true), memberToken).statusCode()).isEqualTo(400);
        assertThat(delete("/api/questions/" + secretQuestionId, otherToken).statusCode()).isEqualTo(404);
        assertThat(delete("/api/questions/" + secretQuestionId, memberToken).statusCode()).isEqualTo(204);
    }

    private void clean() {
        answerRepository.deleteAll();
        questionRepository.deleteAll();
        reviewRepository.deleteAll();
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
        addressRepository.deleteAll();
        memberRepository.deleteAll();
    }

    private Product saveProduct(String code) {
        return productRepository.save(new Product(
                brand,
                category,
                code,
                code + " NAME",
                "SAMPLE SHORT",
                "SAMPLE DESCRIPTION",
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(45000),
                10,
                ProductStatus.ACTIVE,
                false
        ));
    }

    private OrderItem createOrderItem(Member owner, Product product, OrderStatus orderStatus, DeliveryStatus deliveryStatus) {
        Cart cart = cartRepository.findByMemberId(owner.getId()).orElseGet(() -> cartRepository.save(new Cart(owner)));
        CartItem cartItem = cartItemRepository.save(new CartItem(cart, product, 1));
        String orderNumber = orderService.createOrder(owner.getId(), new OrderCreateRequest(
                List.of(cartItem.getId()),
                "SAMPLE RECIPIENT",
                "01012345678",
                "06236",
                "SEOUL TEST ROAD 1",
                "101",
                "LEAVE AT DOOR"
        )).orderNumber();
        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow();
        order.changeStatus(orderStatus);
        orderRepository.save(order);
        Delivery delivery = deliveryRepository.findByOrderId(order.getId()).orElseThrow();
        if (deliveryStatus == DeliveryStatus.PREPARING) {
            delivery.startPreparing();
        } else if (deliveryStatus == DeliveryStatus.SHIPPING) {
            delivery.registerTracking("SAMPLE", "TRACKING");
            delivery.ship(Instant.now());
        } else if (deliveryStatus == DeliveryStatus.DELIVERED) {
            delivery.registerTracking("SAMPLE", "TRACKING");
            delivery.ship(Instant.now());
            delivery.deliver(Instant.now());
        } else if (deliveryStatus == DeliveryStatus.CANCELED) {
            delivery.cancel();
        }
        deliveryRepository.save(delivery);
        return orderItemRepository.findByOrderId(order.getId()).get(0);
    }

    private String reviewJson(Long orderItemId, int rating, String content) {
        return """
                {
                  "orderItemId": %d,
                  "rating": %d,
                  "content": "%s"
                }
                """.formatted(orderItemId, rating, content);
    }

    private String reviewUpdateJson(int rating, String content) {
        return """
                {
                  "rating": %d,
                  "content": "%s"
                }
                """.formatted(rating, content);
    }

    private String questionJson(String title, String content, boolean secret) {
        return """
                {
                  "title": "%s",
                  "content": "%s",
                  "secret": %s
                }
                """.formatted(title, content, secret);
    }

    private String answerJson(String content) {
        return """
                {
                  "content": "%s"
                }
                """.formatted(content);
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

    private HttpResponse<String> put(String path, String json, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri(path))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .PUT(HttpRequest.BodyPublishers.ofString(json));
        applyToken(builder, token);
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> patch(String path, String json, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri(path))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json));
        applyToken(builder, token);
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> delete(String path, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(uri(path)).DELETE();
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
