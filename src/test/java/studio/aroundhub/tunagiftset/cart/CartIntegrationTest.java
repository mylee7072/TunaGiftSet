package studio.aroundhub.tunagiftset.cart;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
import studio.aroundhub.tunagiftset.entity.Product;
import studio.aroundhub.tunagiftset.entity.ProductImage;
import studio.aroundhub.tunagiftset.entity.type.ProductImageType;
import studio.aroundhub.tunagiftset.entity.type.ProductStatus;
import studio.aroundhub.tunagiftset.repository.BrandRepository;
import studio.aroundhub.tunagiftset.repository.CartItemRepository;
import studio.aroundhub.tunagiftset.repository.CartRepository;
import studio.aroundhub.tunagiftset.repository.CategoryRepository;
import studio.aroundhub.tunagiftset.repository.InventoryHistoryRepository;
import studio.aroundhub.tunagiftset.repository.MemberRepository;
import studio.aroundhub.tunagiftset.repository.ProductImageRepository;
import studio.aroundhub.tunagiftset.repository.ProductRepository;
import studio.aroundhub.tunagiftset.security.JwtTokenProvider;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CartIntegrationTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

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
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        productImageRepository.deleteAll();
        inventoryHistoryRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        brandRepository.deleteAll();
        memberRepository.deleteAll();

        member = memberRepository.save(new Member(
                "sample-user@example.com",
                passwordEncoder.encode("SamplePassword123!"),
                "SAMPLE USER",
                "01012345678"
        ));
        anotherMember = memberRepository.save(new Member(
                "another-user@example.com",
                passwordEncoder.encode("SamplePassword123!"),
                "ANOTHER SAMPLE USER",
                "01087654321"
        ));
        memberToken = jwtTokenProvider.createAccessToken(member);
        anotherToken = jwtTokenProvider.createAccessToken(anotherMember);
        brand = brandRepository.save(new Brand("SAMPLE", "SAMPLE BRAND", true));
        category = categoryRepository.save(new Category(null, "SAMPLE CATEGORY", 1, true));
    }

    @Test
    void addItem() throws Exception {
        Product product = saveProduct("SAMPLE-001", ProductStatus.ACTIVE, 100, BigDecimal.valueOf(45000));
        productImageRepository.save(new ProductImage(product, "/images/sample-main.png", ProductImageType.MAIN, 1));

        HttpResponse<String> response = post("/api/cart/items", """
                {
                  "productId": %d,
                  "quantity": 2
                }
                """.formatted(product.getId()), memberToken);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(JsonPath.<Integer>read(response.body(), "$.summary.totalItemCount")).isEqualTo(2);
        assertThat(JsonPath.<Integer>read(response.body(), "$.summary.totalProductAmount")).isEqualTo(90000);
        assertThat(JsonPath.<String>read(response.body(), "$.items[0].thumbnailUrl")).isEqualTo("/images/sample-main.png");
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(100);
    }

    @Test
    void addSameProductIncreasesQuantityWithoutDuplicatedRow() throws Exception {
        Product product = saveProduct("SAMPLE-002", ProductStatus.ACTIVE, 100, BigDecimal.valueOf(10000));

        post("/api/cart/items", """
                {"productId": %d, "quantity": 2}
                """.formatted(product.getId()), memberToken);
        HttpResponse<String> response = post("/api/cart/items", """
                {"productId": %d, "quantity": 3}
                """.formatted(product.getId()), memberToken);

        Cart cart = cartRepository.findByMemberId(member.getId()).orElseThrow();
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(JsonPath.<Integer>read(response.body(), "$.items[0].quantity")).isEqualTo(5);
        assertThat(cartItemRepository.countByCartId(cart.getId())).isEqualTo(1);
        assertThat(cartRepository.existsByMemberId(member.getId())).isTrue();
    }

    @Test
    void addItemFailsWhenProductDoesNotExist() throws Exception {
        HttpResponse<String> response = post("/api/cart/items", """
                {"productId": 999999, "quantity": 1}
                """, memberToken);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo("PRODUCT_NOT_FOUND");
    }

    @Test
    void addItemFailsWhenQuantityIsZeroOrNegative() throws Exception {
        Product product = saveProduct("SAMPLE-003", ProductStatus.ACTIVE, 100, BigDecimal.valueOf(10000));

        HttpResponse<String> zeroResponse = post("/api/cart/items", """
                {"productId": %d, "quantity": 0}
                """.formatted(product.getId()), memberToken);
        HttpResponse<String> negativeResponse = post("/api/cart/items", """
                {"productId": %d, "quantity": -1}
                """.formatted(product.getId()), memberToken);

        assertThat(zeroResponse.statusCode()).isEqualTo(400);
        assertThat(negativeResponse.statusCode()).isEqualTo(400);
    }

    @Test
    void addItemFailsWhenStockIsInsufficient() throws Exception {
        Product product = saveProduct("SAMPLE-004", ProductStatus.ACTIVE, 2, BigDecimal.valueOf(10000));

        HttpResponse<String> response = post("/api/cart/items", """
                {"productId": %d, "quantity": 3}
                """.formatted(product.getId()), memberToken);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo("INSUFFICIENT_STOCK");
    }

    @Test
    void addItemFailsWhenProductStatusIsNotPurchasable() throws Exception {
        Product soldOut = saveProduct("SAMPLE-005", ProductStatus.SOLD_OUT, 10, BigDecimal.valueOf(10000));
        Product hidden = saveProduct("SAMPLE-006", ProductStatus.HIDDEN, 10, BigDecimal.valueOf(10000));
        Product discontinued = saveProduct("SAMPLE-007", ProductStatus.DISCONTINUED, 10, BigDecimal.valueOf(10000));

        assertThat(postAdd(soldOut).statusCode()).isEqualTo(400);
        assertThat(JsonPath.<String>read(postAdd(soldOut).body(), "$.code")).isEqualTo("PRODUCT_SOLD_OUT");
        assertThat(JsonPath.<String>read(postAdd(hidden).body(), "$.code")).isEqualTo("PRODUCT_NOT_AVAILABLE");
        assertThat(JsonPath.<String>read(postAdd(discontinued).body(), "$.code")).isEqualTo("PRODUCT_NOT_AVAILABLE");
    }

    @Test
    void addItemRequiresAuthentication() throws Exception {
        Product product = saveProduct("SAMPLE-008", ProductStatus.ACTIVE, 10, BigDecimal.valueOf(10000));

        HttpResponse<String> response = post("/api/cart/items", """
                {"productId": %d, "quantity": 1}
                """.formatted(product.getId()), null);

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void getEmptyCart() throws Exception {
        HttpResponse<String> response = get("/api/cart", memberToken);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Integer>read(response.body(), "$.items.length()")).isZero();
        assertThat(JsonPath.<Integer>read(response.body(), "$.summary.totalAmount")).isZero();
    }

    @Test
    void getCartCalculatesAmountsAndShippingFee() throws Exception {
        Product product1 = saveProduct("SAMPLE-009", ProductStatus.ACTIVE, 100, BigDecimal.valueOf(10000));
        Product product2 = saveProduct("SAMPLE-010", ProductStatus.ACTIVE, 100, BigDecimal.valueOf(20000));
        addDirect(member, product1, 2);
        addDirect(member, product2, 1);

        HttpResponse<String> response = get("/api/cart", memberToken);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Integer>read(response.body(), "$.items.length()")).isEqualTo(2);
        assertThat(JsonPath.<Integer>read(response.body(), "$.summary.totalItemCount")).isEqualTo(3);
        assertThat(JsonPath.<Integer>read(response.body(), "$.summary.totalProductAmount")).isEqualTo(40000);
        assertThat(JsonPath.<Integer>read(response.body(), "$.summary.shippingFee")).isEqualTo(3000);
        assertThat(JsonPath.<Integer>read(response.body(), "$.summary.totalAmount")).isEqualTo(43000);
    }

    @Test
    void getCartMarksUnavailableItem() throws Exception {
        Product product = saveProduct("SAMPLE-011", ProductStatus.ACTIVE, 10, BigDecimal.valueOf(10000));
        addDirect(member, product, 2);
        product.changeStatus(ProductStatus.SOLD_OUT);
        productRepository.save(product);

        HttpResponse<String> response = get("/api/cart", memberToken);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Boolean>read(response.body(), "$.items[0].available")).isFalse();
        assertThat(JsonPath.<String>read(response.body(), "$.items[0].unavailableReason")).isEqualTo("SOLD_OUT");
        assertThat(JsonPath.<Integer>read(response.body(), "$.summary.totalAmount")).isZero();
    }

    @Test
    void getCartReflectsLatestProductPrice() throws Exception {
        Product product = saveProduct("SAMPLE-012", ProductStatus.ACTIVE, 100, BigDecimal.valueOf(45000));
        addDirect(member, product, 1);
        product.update(brand, category, "SAMPLE-012", "SAMPLE PRODUCT", "SAMPLE", "SAMPLE", BigDecimal.valueOf(50000), BigDecimal.valueOf(40000), 100, ProductStatus.ACTIVE, false);
        productRepository.save(product);

        HttpResponse<String> response = get("/api/cart", memberToken);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Integer>read(response.body(), "$.items[0].salePrice")).isEqualTo(40000);
        assertThat(JsonPath.<Integer>read(response.body(), "$.summary.totalProductAmount")).isEqualTo(40000);
    }

    @Test
    void updateQuantity() throws Exception {
        Product product = saveProduct("SAMPLE-013", ProductStatus.ACTIVE, 100, BigDecimal.valueOf(10000));
        CartItem cartItem = addDirect(member, product, 1);

        HttpResponse<String> response = patch("/api/cart/items/" + cartItem.getId(), """
                {"quantity": 5}
                """, memberToken);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Integer>read(response.body(), "$.items[0].quantity")).isEqualTo(5);
    }

    @Test
    void updateQuantityFailsWhenStockExceededOrQuantityZero() throws Exception {
        Product product = saveProduct("SAMPLE-014", ProductStatus.ACTIVE, 2, BigDecimal.valueOf(10000));
        CartItem cartItem = addDirect(member, product, 1);

        HttpResponse<String> stockResponse = patch("/api/cart/items/" + cartItem.getId(), """
                {"quantity": 3}
                """, memberToken);
        HttpResponse<String> zeroResponse = patch("/api/cart/items/" + cartItem.getId(), """
                {"quantity": 0}
                """, memberToken);

        assertThat(stockResponse.statusCode()).isEqualTo(400);
        assertThat(JsonPath.<String>read(stockResponse.body(), "$.code")).isEqualTo("INSUFFICIENT_STOCK");
        assertThat(zeroResponse.statusCode()).isEqualTo(400);
    }

    @Test
    void updateQuantityFailsWhenCartItemDoesNotExistOrBelongsToAnotherMember() throws Exception {
        Product product = saveProduct("SAMPLE-015", ProductStatus.ACTIVE, 10, BigDecimal.valueOf(10000));
        CartItem otherItem = addDirect(anotherMember, product, 1);

        HttpResponse<String> missingResponse = patch("/api/cart/items/999999", """
                {"quantity": 1}
                """, memberToken);
        HttpResponse<String> otherResponse = patch("/api/cart/items/" + otherItem.getId(), """
                {"quantity": 1}
                """, memberToken);

        assertThat(missingResponse.statusCode()).isEqualTo(404);
        assertThat(otherResponse.statusCode()).isEqualTo(404);
        assertThat(JsonPath.<String>read(otherResponse.body(), "$.code")).isEqualTo("CART_ITEM_NOT_FOUND");
    }

    @Test
    void deleteSingleItemAndRejectAnotherMemberItem() throws Exception {
        Product product = saveProduct("SAMPLE-016", ProductStatus.ACTIVE, 10, BigDecimal.valueOf(10000));
        Product otherProduct = saveProduct("SAMPLE-017", ProductStatus.ACTIVE, 10, BigDecimal.valueOf(10000));
        CartItem item = addDirect(member, product, 1);
        CartItem otherItem = addDirect(anotherMember, otherProduct, 1);

        HttpResponse<String> otherResponse = delete("/api/cart/items/" + otherItem.getId(), null, memberToken);
        HttpResponse<String> response = delete("/api/cart/items/" + item.getId(), null, memberToken);

        assertThat(otherResponse.statusCode()).isEqualTo(404);
        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(cartItemRepository.findById(item.getId())).isEmpty();
        assertThat(cartItemRepository.findById(otherItem.getId())).isPresent();
    }

    @Test
    void deleteSelectedItemsAndClearCart() throws Exception {
        Product product1 = saveProduct("SAMPLE-018", ProductStatus.ACTIVE, 10, BigDecimal.valueOf(10000));
        Product product2 = saveProduct("SAMPLE-019", ProductStatus.ACTIVE, 10, BigDecimal.valueOf(10000));
        Product product3 = saveProduct("SAMPLE-020", ProductStatus.ACTIVE, 10, BigDecimal.valueOf(10000));
        CartItem item1 = addDirect(member, product1, 1);
        CartItem item2 = addDirect(member, product2, 1);
        addDirect(member, product3, 1);

        HttpResponse<String> selectedDeleteResponse = delete("/api/cart/items", """
                {"cartItemIds": [%d, %d]}
                """.formatted(item1.getId(), item2.getId()), memberToken);

        assertThat(selectedDeleteResponse.statusCode()).isEqualTo(204);
        assertThat(cartItemRepository.findById(item1.getId())).isEmpty();
        assertThat(cartItemRepository.findById(item2.getId())).isEmpty();

        HttpResponse<String> clearResponse = delete("/api/cart/items", null, memberToken);
        Cart cart = cartRepository.findByMemberId(member.getId()).orElseThrow();

        assertThat(clearResponse.statusCode()).isEqualTo(204);
        assertThat(cartItemRepository.countByCartId(cart.getId())).isZero();
    }

    private HttpResponse<String> postAdd(Product product) throws Exception {
        return post("/api/cart/items", """
                {"productId": %d, "quantity": 1}
                """.formatted(product.getId()), memberToken);
    }

    private Product saveProduct(String code, ProductStatus status, int stockQuantity, BigDecimal salePrice) {
        return productRepository.save(new Product(
                brand,
                category,
                code,
                "SAMPLE PRODUCT",
                "SAMPLE",
                "SAMPLE PRODUCT DATA",
                salePrice,
                salePrice,
                stockQuantity,
                status,
                false
        ));
    }

    private CartItem addDirect(Member owner, Product product, int quantity) {
        Cart cart = cartRepository.findByMemberId(owner.getId())
                .orElseGet(() -> cartRepository.save(new Cart(owner)));
        CartItem cartItem = new CartItem(cart, product, quantity);
        CartItem savedCartItem = cartItemRepository.save(cartItem);
        ReflectionTestUtils.setField(savedCartItem, "cart", cart);
        ReflectionTestUtils.setField(savedCartItem, "product", product);
        return savedCartItem;
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

    private HttpResponse<String> patch(String path, String json, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri(path))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json));
        applyToken(builder, token);
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> delete(String path, String json, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri(path));
        if (json == null) {
            builder.DELETE();
        } else {
            builder.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .method("DELETE", HttpRequest.BodyPublishers.ofString(json));
        }
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
