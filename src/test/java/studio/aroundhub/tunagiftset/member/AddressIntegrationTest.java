package studio.aroundhub.tunagiftset.member;

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
import studio.aroundhub.tunagiftset.entity.Address;
import studio.aroundhub.tunagiftset.entity.Brand;
import studio.aroundhub.tunagiftset.entity.Cart;
import studio.aroundhub.tunagiftset.entity.CartItem;
import studio.aroundhub.tunagiftset.entity.Category;
import studio.aroundhub.tunagiftset.entity.Member;
import studio.aroundhub.tunagiftset.entity.Product;
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
import studio.aroundhub.tunagiftset.repository.ProductRepository;
import studio.aroundhub.tunagiftset.security.JwtTokenProvider;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AddressIntegrationTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private AddressRepository addressRepository;

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
        addressRepository.deleteAll();
        memberRepository.deleteAll();

        member = memberRepository.save(new Member(
                "address-user@example.com",
                passwordEncoder.encode("SamplePassword123!"),
                "ADDRESS USER",
                "01012345678"
        ));
        anotherMember = memberRepository.save(new Member(
                "address-other@example.com",
                passwordEncoder.encode("SamplePassword123!"),
                "ADDRESS OTHER",
                "01087654321"
        ));
        memberToken = jwtTokenProvider.createAccessToken(member);
        anotherToken = jwtTokenProvider.createAccessToken(anotherMember);
        brand = brandRepository.save(new Brand("ADDRESS-BRAND", "ADDRESS BRAND", true));
        category = categoryRepository.save(new Category(null, "ADDRESS CATEGORY", 1, true));
    }

    @AfterEach
    void tearDown() {
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

    @Test
    void createListUpdateDefaultAndDeleteAddress() throws Exception {
        HttpResponse<String> first = post("/api/members/me/addresses", addressJson("HOME", "SAMPLE RECIPIENT", "010-1234-5678", "06236", "SEOUL GANGNAM TEST ROAD 1", "101", false), memberToken);
        HttpResponse<String> second = post("/api/members/me/addresses", addressJson("OFFICE", "SECOND RECIPIENT", "01099998888", "04524", "SEOUL JUNG TEST ROAD 110", "5F", true), memberToken);

        Long firstId = JsonPath.<Number>read(first.body(), "$.id").longValue();
        Long secondId = JsonPath.<Number>read(second.body(), "$.id").longValue();

        assertThat(first.statusCode()).isEqualTo(201);
        assertThat(JsonPath.<Boolean>read(first.body(), "$.defaultAddress")).isTrue();
        assertThat(second.statusCode()).isEqualTo(201);
        assertThat(JsonPath.<Boolean>read(second.body(), "$.defaultAddress")).isTrue();
        assertThat(defaultCount(member.getId())).isEqualTo(1);

        HttpResponse<String> list = get("/api/members/me/addresses", memberToken);
        assertThat(list.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Integer>read(list.body(), "$.length()")).isEqualTo(2);
        assertThat(JsonPath.<Number>read(list.body(), "$[0].id").longValue()).isEqualTo(secondId);

        HttpResponse<String> updated = put("/api/members/me/addresses/" + firstId, addressJson("PARENTS", "UPDATED RECIPIENT", "01022223333", "48058", "BUSAN TEST ROAD 97", "202", false), memberToken);
        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(updated.body(), "$.recipientPhone")).isEqualTo("01022223333");
        assertThat(defaultCount(member.getId())).isEqualTo(1);

        HttpResponse<String> defaultChanged = patch("/api/members/me/addresses/" + firstId + "/default", "{}", memberToken);
        assertThat(defaultChanged.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Boolean>read(defaultChanged.body(), "$.defaultAddress")).isTrue();
        assertThat(defaultCount(member.getId())).isEqualTo(1);

        HttpResponse<String> deleted = delete("/api/members/me/addresses/" + firstId, memberToken);
        assertThat(deleted.statusCode()).isEqualTo(204);
        assertThat(defaultCount(member.getId())).isEqualTo(1);
        assertThat(addressRepository.findById(secondId).orElseThrow().isDefaultAddress()).isTrue();
    }

    @Test
    void addressValidationAndLimit() throws Exception {
        Product product = saveProduct();

        HttpResponse<String> invalid = post("/api/members/me/addresses", """
                {
                  "addressName": "HOME",
                  "recipientName": "",
                  "recipientPhone": "02-123-4567",
                  "zipCode": "1234",
                  "roadAddress": "",
                  "detailAddress": ""
                }
                """, memberToken);
        assertThat(invalid.statusCode()).isEqualTo(400);
        assertThat(JsonPath.<String>read(invalid.body(), "$.code")).isEqualTo("VALIDATION_FAILED");

        for (int i = 0; i < 20; i++) {
            assertThat(post("/api/members/me/addresses", addressJson("ADDRESS" + i, "SAMPLE RECIPIENT", "0101234%04d".formatted(i), "06236", "SEOUL TEST ROAD " + i, i + "F", false), memberToken).statusCode()).isEqualTo(201);
        }
        HttpResponse<String> exceeded = post("/api/members/me/addresses", addressJson("EXCEEDED", "SAMPLE RECIPIENT", "01077778888", "06236", "SEOUL TEST ROAD 99", "99", false), memberToken);
        assertThat(exceeded.statusCode()).isEqualTo(400);
        assertThat(JsonPath.<String>read(exceeded.body(), "$.code")).isEqualTo("ADDRESS_LIMIT_EXCEEDED");

        assertThat(product.getId()).isNotNull();
    }

    @Test
    void addressApisRequireAuthenticationAndProtectOwnership() throws Exception {
        Long addressId = saveAddress(member, "HOME", true).getId();

        assertThat(get("/api/members/me/addresses", null).statusCode()).isEqualTo(401);
        assertThat(post("/api/members/me/addresses", addressJson("HOME", "SAMPLE RECIPIENT", "01012345678", "06236", "SEOUL TEST ROAD 1", "101", false), null).statusCode()).isEqualTo(401);
        assertThat(put("/api/members/me/addresses/" + addressId, addressJson("ATTACK", "OTHER RECIPIENT", "01011112222", "04524", "SEOUL TEST ROAD 110", "1F", false), anotherToken).statusCode()).isEqualTo(404);
        assertThat(patch("/api/members/me/addresses/" + addressId + "/default", "{}", anotherToken).statusCode()).isEqualTo(404);
        assertThat(delete("/api/members/me/addresses/" + addressId, anotherToken).statusCode()).isEqualTo(404);
        assertThat(addressRepository.findById(addressId)).isPresent();
    }

    @Test
    void orderUsesAddressSnapshotAndRejectsOtherMembersAddressId() throws Exception {
        Address address = saveAddress(member, "HOME", true);
        Address otherAddress = saveAddress(anotherMember, "OTHER HOME", true);
        CartItem cartItem = addCartItem(member, saveProduct(), 1);

        HttpResponse<String> attack = post("/api/orders", """
                {
                  "cartItemIds": [%d],
                  "addressId": %d,
                  "deliveryMessage": "LEAVE AT DOOR"
                }
                """.formatted(cartItem.getId(), otherAddress.getId()), memberToken);
        assertThat(attack.statusCode()).isEqualTo(404);
        assertThat(JsonPath.<String>read(attack.body(), "$.code")).isEqualTo("ADDRESS_NOT_FOUND");

        HttpResponse<String> created = post("/api/orders", """
                {
                  "cartItemIds": [%d],
                  "addressId": %d,
                  "deliveryMessage": "LEAVE AT DOOR"
                }
                """.formatted(cartItem.getId(), address.getId()), memberToken);
        assertThat(created.statusCode()).isEqualTo(200);
        String orderNumber = JsonPath.read(created.body(), "$.orderNumber");
        assertThat(JsonPath.<String>read(created.body(), "$.recipientPhone")).isEqualTo("01012345678");
        assertThat(JsonPath.<String>read(created.body(), "$.roadAddress")).isEqualTo("SEOUL GANGNAM TEST ROAD 1");
        assertThat(JsonPath.<String>read(created.body(), "$.detailAddress")).isEqualTo("101");
        assertThat(JsonPath.<String>read(created.body(), "$.extraAddress")).isEqualTo("(YEOKSAM)");

        put("/api/members/me/addresses/" + address.getId(), addressJson("UPDATED", "UPDATED RECIPIENT", "01099998888", "48058", "BUSAN TEST ROAD 97", "202", false), memberToken);
        HttpResponse<String> afterUpdate = get("/api/orders/" + orderNumber, memberToken);
        assertThat(JsonPath.<String>read(afterUpdate.body(), "$.recipientName")).isEqualTo("SAMPLE RECIPIENT");
        assertThat(JsonPath.<String>read(afterUpdate.body(), "$.roadAddress")).isEqualTo("SEOUL GANGNAM TEST ROAD 1");

        delete("/api/members/me/addresses/" + address.getId(), memberToken);
        HttpResponse<String> afterDelete = get("/api/orders/" + orderNumber, memberToken);
        assertThat(afterDelete.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(afterDelete.body(), "$.detailAddress")).isEqualTo("101");
    }

    @Test
    void concurrentDefaultChangesLeaveOnlyOneDefaultAddress() throws Exception {
        Address first = saveAddress(member, "HOME", true);
        Address second = saveAddress(member, "OFFICE", false);

        CountDownLatch startLatch = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        executor.submit(() -> {
            startLatch.await(5, TimeUnit.SECONDS);
            patch("/api/members/me/addresses/" + first.getId() + "/default", "{}", memberToken);
            return null;
        });
        executor.submit(() -> {
            startLatch.await(5, TimeUnit.SECONDS);
            patch("/api/members/me/addresses/" + second.getId() + "/default", "{}", memberToken);
            return null;
        });
        startLatch.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(defaultCount(member.getId())).isEqualTo(1);
    }

    private long defaultCount(Long memberId) {
        return addressRepository.findByMemberId(memberId).stream()
                .filter(Address::isDefaultAddress)
                .count();
    }

    private Address saveAddress(Member owner, String name, boolean defaultAddress) {
        return addressRepository.save(new Address(
                owner,
                name,
                "SAMPLE RECIPIENT",
                "01012345678",
                "06236",
                "SEOUL GANGNAM TEST ROAD 1",
                "SEOUL GANGNAM JIBUN 1",
                "101",
                "(YEOKSAM)",
                defaultAddress
        ));
    }

    private Product saveProduct() {
        return productRepository.save(new Product(
                brand,
                category,
                "ADDRESS-PRODUCT-" + System.nanoTime(),
                "ADDRESS PRODUCT",
                "SAMPLE",
                "ADDRESS PRODUCT DATA",
                BigDecimal.valueOf(30000),
                BigDecimal.valueOf(30000),
                10,
                ProductStatus.ACTIVE,
                false
        ));
    }

    private CartItem addCartItem(Member owner, Product product, int quantity) {
        Cart cart = cartRepository.findByMemberId(owner.getId())
                .orElseGet(() -> cartRepository.save(new Cart(owner)));
        return cartItemRepository.save(new CartItem(cart, product, quantity));
    }

    private String addressJson(String addressName, String recipientName, String recipientPhone, String zipCode, String roadAddress, String detailAddress, boolean defaultAddress) {
        return """
                {
                  "addressName": "%s",
                  "recipientName": "%s",
                  "recipientPhone": "%s",
                  "zipCode": "%s",
                  "roadAddress": "%s",
                  "jibunAddress": "SEOUL GANGNAM JIBUN 1",
                  "detailAddress": "%s",
                  "extraAddress": "(YEOKSAM)",
                  "defaultAddress": %s
                }
                """.formatted(addressName, recipientName, recipientPhone, zipCode, roadAddress, detailAddress, defaultAddress);
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
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri(path))
                .DELETE();
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
