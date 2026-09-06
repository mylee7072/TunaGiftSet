package studio.aroundhub.tunagiftset.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
import studio.aroundhub.tunagiftset.entity.Category;
import studio.aroundhub.tunagiftset.entity.Member;
import studio.aroundhub.tunagiftset.entity.Product;
import studio.aroundhub.tunagiftset.entity.ProductImage;
import studio.aroundhub.tunagiftset.entity.type.ProductImageType;
import studio.aroundhub.tunagiftset.entity.type.ProductStatus;
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
import studio.aroundhub.tunagiftset.repository.WishlistRepository;
import studio.aroundhub.tunagiftset.security.JwtTokenProvider;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WishlistIntegrationTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private WishlistRepository wishlistRepository;

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
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductQuestionRepository questionRepository;

    @Autowired
    private ProductQuestionAnswerRepository answerRepository;

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
    private String memberToken;
    private String otherToken;
    private Brand brand;
    private Category category;

    @BeforeEach
    void setUp() {
        clean();
        member = memberRepository.save(new Member("wishlist-user@example.com", passwordEncoder.encode("SamplePassword123!"), "SAMPLE USER", "01012345678"));
        otherMember = memberRepository.save(new Member("wishlist-other@example.com", passwordEncoder.encode("SamplePassword123!"), "OTHER USER", "01087654321"));
        memberToken = jwtTokenProvider.createAccessToken(member);
        otherToken = jwtTokenProvider.createAccessToken(otherMember);
        brand = brandRepository.save(new Brand("WISHLIST-BRAND", "WISHLIST BRAND", true));
        category = categoryRepository.save(new Category(null, "WISHLIST CATEGORY", 1, true));
    }

    @AfterEach
    void tearDown() {
        clean();
    }

    @Test
    void wishlistApisRequireAuthenticationAndUseCurrentMember() throws Exception {
        Product product = saveProduct("WISHLIST-PRODUCT-1", ProductStatus.ACTIVE, 10);

        assertThat(post("/api/products/" + product.getId() + "/wishlist", null).statusCode()).isEqualTo(401);
        assertThat(delete("/api/products/" + product.getId() + "/wishlist", null).statusCode()).isEqualTo(401);
        assertThat(get("/api/members/me/wishlist", null).statusCode()).isEqualTo(401);

        assertThat(post("/api/products/" + product.getId() + "/wishlist", memberToken).statusCode()).isEqualTo(200);
        HttpResponse<String> otherList = get("/api/members/me/wishlist", otherToken);
        assertThat(otherList.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Integer>read(otherList.body(), "$.content.length()")).isZero();
    }

    @Test
    void addAndRemoveWishlistAreIdempotentAndIsolatedByMember() throws Exception {
        Product product = saveProduct("WISHLIST-PRODUCT-2", ProductStatus.ACTIVE, 10);

        HttpResponse<String> first = post("/api/products/" + product.getId() + "/wishlist", memberToken);
        HttpResponse<String> second = post("/api/products/" + product.getId() + "/wishlist", memberToken);

        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(second.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Boolean>read(second.body(), "$.wishlisted")).isTrue();
        assertThat(wishlistRepository.findAll()).hasSize(1);

        assertThat(post("/api/products/" + product.getId() + "/wishlist", otherToken).statusCode()).isEqualTo(200);
        assertThat(wishlistRepository.findAll()).hasSize(2);

        HttpResponse<String> removed = delete("/api/products/" + product.getId() + "/wishlist", memberToken);
        HttpResponse<String> removedAgain = delete("/api/products/" + product.getId() + "/wishlist", memberToken);

        assertThat(removed.statusCode()).isEqualTo(200);
        assertThat(removedAgain.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Boolean>read(removedAgain.body(), "$.wishlisted")).isFalse();
        assertThat(wishlistRepository.existsByMemberIdAndProductId(otherMember.getId(), product.getId())).isTrue();
    }

    @Test
    void productStatusPolicyAllowsSoldOutButBlocksHiddenAndDiscontinued() throws Exception {
        Product active = saveProduct("WISHLIST-ACTIVE", ProductStatus.ACTIVE, 10);
        Product soldOut = saveProduct("WISHLIST-SOLDOUT", ProductStatus.SOLD_OUT, 0);
        Product hidden = saveProduct("WISHLIST-HIDDEN", ProductStatus.HIDDEN, 10);
        Product discontinued = saveProduct("WISHLIST-DISCONTINUED", ProductStatus.DISCONTINUED, 10);

        assertThat(post("/api/products/" + active.getId() + "/wishlist", memberToken).statusCode()).isEqualTo(200);
        assertThat(post("/api/products/" + soldOut.getId() + "/wishlist", memberToken).statusCode()).isEqualTo(200);
        assertThat(post("/api/products/" + hidden.getId() + "/wishlist", memberToken).statusCode()).isEqualTo(400);
        assertThat(post("/api/products/" + discontinued.getId() + "/wishlist", memberToken).statusCode()).isEqualTo(400);
    }

    @Test
    void duplicateConcurrentPostsCreateSingleWishlistRow() throws Exception {
        Product product = saveProduct("WISHLIST-PRODUCT-3", ProductStatus.ACTIVE, 10);
        CountDownLatch startLatch = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        var first = executor.submit(() -> {
            startLatch.await(5, TimeUnit.SECONDS);
            return post("/api/products/" + product.getId() + "/wishlist", memberToken).statusCode();
        });
        var second = executor.submit(() -> {
            startLatch.await(5, TimeUnit.SECONDS);
            return post("/api/products/" + product.getId() + "/wishlist", memberToken).statusCode();
        });

        startLatch.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(List.of(first.get(), second.get())).containsOnly(200);
        assertThat(wishlistRepository.findAll()).hasSize(1);
    }

    @Test
    void myWishlistListUsesCurrentPriceThumbnailPaginationAndExcludesHiddenProducts() throws Exception {
        Product first = saveProduct("WISHLIST-PRODUCT-4", ProductStatus.ACTIVE, 10);
        Product second = saveProduct("WISHLIST-PRODUCT-5", ProductStatus.SOLD_OUT, 0);
        Product hidden = saveProduct("WISHLIST-PRODUCT-6", ProductStatus.HIDDEN, 10);
        productImageRepository.save(new ProductImage(first, "/sample-image.png", ProductImageType.MAIN, 0));

        post("/api/products/" + first.getId() + "/wishlist", memberToken);
        post("/api/products/" + second.getId() + "/wishlist", memberToken);
        wishlistRepository.saveAndFlush(new studio.aroundhub.tunagiftset.entity.Wishlist(member, hidden));

        HttpResponse<String> list = get("/api/members/me/wishlist?page=0&size=20", memberToken);

        assertThat(list.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Integer>read(list.body(), "$.content.length()")).isEqualTo(2);
        assertThat(list.body()).contains("/sample-image.png");
        assertThat(list.body()).contains("45000");
        assertThat(list.body()).contains("SOLD_OUT");
        assertThat(list.body()).doesNotContain("WISHLIST-PRODUCT-6 NAME");
    }

    @Test
    void statusAndBatchProductIdsAreCurrentMemberScopedAndProductCountIsReturned() throws Exception {
        Product first = saveProduct("WISHLIST-PRODUCT-7", ProductStatus.ACTIVE, 10);
        Product second = saveProduct("WISHLIST-PRODUCT-8", ProductStatus.ACTIVE, 10);
        post("/api/products/" + first.getId() + "/wishlist", memberToken);
        post("/api/products/" + first.getId() + "/wishlist", otherToken);
        post("/api/products/" + second.getId() + "/wishlist", otherToken);

        HttpResponse<String> firstStatus = get("/api/members/me/wishlist/products/" + first.getId() + "/status", memberToken);
        HttpResponse<String> secondStatus = get("/api/members/me/wishlist/products/" + second.getId() + "/status", memberToken);
        HttpResponse<String> batch = postJson("/api/members/me/wishlist/product-ids", "{\"productIds\":[" + first.getId() + "," + second.getId() + "]}", memberToken);
        HttpResponse<String> productDetail = get("/api/products/" + first.getId(), null);

        assertThat(JsonPath.<Boolean>read(firstStatus.body(), "$.wishlisted")).isTrue();
        assertThat(JsonPath.<Integer>read(firstStatus.body(), "$.wishlistCount")).isEqualTo(2);
        assertThat(JsonPath.<Boolean>read(secondStatus.body(), "$.wishlisted")).isFalse();
        assertThat(JsonPath.<List<Integer>>read(batch.body(), "$.productIds")).containsExactly(first.getId().intValue());
        assertThat(JsonPath.<Integer>read(productDetail.body(), "$.wishlistCount")).isEqualTo(2);
    }

    @Test
    void missingProductReturnsNotFound() throws Exception {
        assertThat(post("/api/products/999999/wishlist", memberToken).statusCode()).isEqualTo(404);
    }

    private void clean() {
        wishlistRepository.deleteAll();
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

    private Product saveProduct(String code, ProductStatus status, int stockQuantity) {
        return productRepository.save(new Product(
                brand,
                category,
                code,
                code + " NAME",
                "SAMPLE SHORT",
                "SAMPLE DESCRIPTION",
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(45000),
                stockQuantity,
                status,
                false
        ));
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(uri(path)).GET();
        applyToken(builder, token);
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(uri(path)).POST(HttpRequest.BodyPublishers.noBody());
        applyToken(builder, token);
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postJson(String path, String json, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri(path))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(json));
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
