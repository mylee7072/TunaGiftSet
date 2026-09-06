package studio.aroundhub.tunagiftset.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import studio.aroundhub.tunagiftset.entity.type.MemberRole;
import studio.aroundhub.tunagiftset.entity.type.MemberStatus;
import studio.aroundhub.tunagiftset.entity.type.ProductImageType;
import studio.aroundhub.tunagiftset.entity.type.ProductStatus;
import studio.aroundhub.tunagiftset.repository.BrandRepository;
import studio.aroundhub.tunagiftset.repository.CategoryRepository;
import studio.aroundhub.tunagiftset.repository.MemberRepository;
import studio.aroundhub.tunagiftset.repository.ProductImageRepository;
import studio.aroundhub.tunagiftset.repository.ProductRepository;
import studio.aroundhub.tunagiftset.security.JwtTokenProvider;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductImageIntegrationTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @LocalServerPort
    private int port;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Product product;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        productImageRepository.deleteAll();

        String suffix = UUID.randomUUID().toString();
        Brand brand = brandRepository.save(new Brand("SAMPLE-" + suffix, "SAMPLE BRAND", true));
        Category category = categoryRepository.save(new Category(null, "SAMPLE CATEGORY " + suffix, 1, true));
        product = productRepository.save(new Product(
                brand,
                category,
                "SAMPLE-" + suffix,
                "SAMPLE GIFT SET",
                "테스트용 SAMPLE 상품입니다.",
                "실제 판매 상품이 아닌 SAMPLE 데이터입니다.",
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(45000),
                100,
                ProductStatus.ACTIVE,
                true
        ));

        Member admin = memberRepository.save(new Member(
                "admin-image-" + suffix + "@example.com",
                passwordEncoder.encode("SamplePassword123!"),
                "샘플관리자",
                "01012345678",
                MemberRole.ADMIN,
                MemberStatus.ACTIVE
        ));
        Member user = memberRepository.save(new Member(
                "user-image-" + suffix + "@example.com",
                passwordEncoder.encode("SamplePassword123!"),
                "샘플회원",
                "01012345679"
        ));
        adminToken = jwtTokenProvider.createAccessToken(admin);
        userToken = jwtTokenProvider.createAccessToken(user);
    }

    @Test
    void anonymousCannotUploadProductImage() throws Exception {
        HttpResponse<String> response = upload("image.jpg", "image/jpeg", ProductImageType.MAIN, null);

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void userCannotUploadProductImage() throws Exception {
        HttpResponse<String> response = upload("image.jpg", "image/jpeg", ProductImageType.MAIN, userToken);

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void adminUploadsFirstImageAsMain() throws Exception {
        HttpResponse<String> response = upload("image.jpg", "image/jpeg", ProductImageType.DETAIL, adminToken);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(JsonPath.<String>read(response.body(), "$.imageType")).isEqualTo("MAIN");
        assertThat(JsonPath.<String>read(response.body(), "$.contentType")).isEqualTo("image/jpeg");
        assertThat(JsonPath.<Integer>read(response.body(), "$.fileSize")).isEqualTo(4);

        ProductImage image = productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId()).get(0);
        assertThat(image.getObjectKey()).startsWith("products/" + product.getId() + "/");
        assertThat(image.getObjectKey()).endsWith(".jpg");
        assertThat(image.getOriginalFilename()).isEqualTo("image.jpg");
    }

    @Test
    void adminRejectsInvalidImageTypeAndSvg() throws Exception {
        HttpResponse<String> exeResponse = upload("evil.exe", "application/octet-stream", ProductImageType.MAIN, adminToken);
        assertThat(exeResponse.statusCode()).isEqualTo(400);
        assertThat(JsonPath.<String>read(exeResponse.body(), "$.code")).isEqualTo("INVALID_IMAGE_TYPE");

        HttpResponse<String> svgResponse = upload("vector.svg", "image/svg+xml", ProductImageType.MAIN, adminToken);
        assertThat(svgResponse.statusCode()).isEqualTo(400);
        assertThat(JsonPath.<String>read(svgResponse.body(), "$.code")).isEqualTo("INVALID_IMAGE_TYPE");
    }

    @Test
    void adminRejectsEmptyAndTooLargeImage() throws Exception {
        HttpResponse<String> emptyResponse = upload("empty.jpg", "image/jpeg", ProductImageType.MAIN, adminToken, new byte[0]);
        assertThat(emptyResponse.statusCode()).isEqualTo(400);
        assertThat(JsonPath.<String>read(emptyResponse.body(), "$.code")).isEqualTo("EMPTY_IMAGE_FILE");

        byte[] tooLarge = new byte[(5 * 1024 * 1024) + 1];
        HttpResponse<String> largeResponse = upload("large.jpg", "image/jpeg", ProductImageType.MAIN, adminToken, tooLarge);
        assertThat(largeResponse.statusCode()).isEqualTo(400);
        assertThat(JsonPath.<String>read(largeResponse.body(), "$.code")).isEqualTo("IMAGE_FILE_TOO_LARGE");
    }

    @Test
    void adminKeepsOnlyOneMainImage() throws Exception {
        long firstId = uploadAndReadId("first.jpg", ProductImageType.MAIN);
        long secondId = uploadAndReadId("second.png", ProductImageType.MAIN);

        assertThat(productImageRepository.findById(firstId).orElseThrow().getImageType()).isEqualTo(ProductImageType.DETAIL);
        assertThat(productImageRepository.findById(secondId).orElseThrow().getImageType()).isEqualTo(ProductImageType.MAIN);
    }

    @Test
    void adminChangesMainImage() throws Exception {
        long firstId = uploadAndReadId("first.jpg", ProductImageType.MAIN);
        long secondId = uploadAndReadId("second.png", ProductImageType.DETAIL);

        HttpResponse<String> response = patch("/api/admin/products/" + product.getId() + "/images/" + secondId + "/main", adminToken);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(response.body(), "$.imageType")).isEqualTo("MAIN");
        assertThat(productImageRepository.findById(firstId).orElseThrow().getImageType()).isEqualTo(ProductImageType.DETAIL);
        assertThat(productImageRepository.findById(secondId).orElseThrow().getImageType()).isEqualTo(ProductImageType.MAIN);
    }

    @Test
    void adminCannotManipulateOtherProductsImage() throws Exception {
        long imageId = uploadAndReadId("first.jpg", ProductImageType.MAIN);
        Product otherProduct = productRepository.save(new Product(
                product.getBrand(),
                product.getCategory(),
                "SAMPLE-002",
                "OTHER SAMPLE",
                null,
                null,
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(9000),
                10,
                ProductStatus.ACTIVE,
                false
        ));

        HttpResponse<String> response = patch("/api/admin/products/" + otherProduct.getId() + "/images/" + imageId + "/main", adminToken);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo("IMAGE_NOT_FOUND");
    }

    @Test
    void adminReordersImages() throws Exception {
        long firstId = uploadAndReadId("first.jpg", ProductImageType.MAIN);
        long secondId = uploadAndReadId("second.png", ProductImageType.DETAIL);
        long thirdId = uploadAndReadId("third.webp", ProductImageType.DETAIL);

        HttpResponse<String> response = putJson(
                "/api/admin/products/" + product.getId() + "/images/order",
                objectMapper.writeValueAsString(Map.of("imageIds", List.of(thirdId, firstId, secondId))),
                adminToken
        );

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Integer>read(response.body(), "$[0].id").longValue()).isEqualTo(thirdId);
        assertThat(JsonPath.<Integer>read(response.body(), "$[0].displayOrder")).isEqualTo(0);
    }

    @Test
    void deletingMainImagePromotesNextImage() throws Exception {
        long firstId = uploadAndReadId("first.jpg", ProductImageType.MAIN);
        long secondId = uploadAndReadId("second.png", ProductImageType.DETAIL);

        HttpResponse<String> response = delete("/api/admin/products/" + product.getId() + "/images/" + firstId, adminToken);

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(productImageRepository.findById(firstId)).isEmpty();
        assertThat(productImageRepository.findById(secondId).orElseThrow().getImageType()).isEqualTo(ProductImageType.MAIN);
    }

    @Test
    void tooManyImagesAreRejected() throws Exception {
        for (int i = 0; i < 10; i++) {
            uploadAndReadId("image-" + i + ".jpg", ProductImageType.DETAIL);
        }

        HttpResponse<String> response = upload("over.jpg", "image/jpeg", ProductImageType.DETAIL, adminToken);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo("TOO_MANY_PRODUCT_IMAGES");
    }

    @Test
    void productListReturnsMainThumbnailUrl() throws Exception {
        uploadAndReadId("main.jpg", ProductImageType.MAIN);

        HttpResponse<String> response = get("/api/products?keyword=SAMPLE%20GIFT%20SET", null);

        assertThat(response.statusCode()).isEqualTo(200);
        List<String> thumbnails = JsonPath.read(response.body(), "$.content[?(@.id==" + product.getId() + ")].thumbnailImageUrl");
        assertThat(thumbnails).hasSize(1);
        assertThat(thumbnails.get(0)).contains("/uploads/products/" + product.getId() + "/");
    }

    @Test
    void productDetailReturnsImagesInDisplayOrder() throws Exception {
        long firstId = uploadAndReadId("first.jpg", ProductImageType.MAIN);
        long secondId = uploadAndReadId("second.png", ProductImageType.DETAIL);
        putJson(
                "/api/admin/products/" + product.getId() + "/images/order",
                objectMapper.writeValueAsString(Map.of("imageIds", List.of(secondId, firstId))),
                adminToken
        );

        HttpResponse<String> response = get("/api/products/" + product.getId(), null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Integer>read(response.body(), "$.images[0].id").longValue()).isEqualTo(secondId);
        assertThat(JsonPath.<String>read(response.body(), "$.images[0].imageUrl")).contains("/uploads/products/" + product.getId() + "/");
    }

    private long uploadAndReadId(String filename, ProductImageType imageType) throws Exception {
        HttpResponse<String> response = upload(filename, contentTypeFor(filename), imageType, adminToken);
        assertThat(response.statusCode()).isEqualTo(201);
        return JsonPath.<Number>read(response.body(), "$.id").longValue();
    }

    private HttpResponse<String> upload(String filename, String contentType, ProductImageType imageType, String token) throws Exception {
        return upload(filename, contentType, imageType, token, new byte[] {1, 2, 3, 4});
    }

    private HttpResponse<String> upload(
            String filename,
            String contentType,
            ProductImageType imageType,
            String token,
            byte[] content
    ) throws Exception {
        String boundary = "----TunaGiftSet" + UUID.randomUUID();
        byte[] body = multipartBody(boundary, filename, contentType, imageType, content);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri("/api/admin/products/" + product.getId() + "/images"))
                .header(HttpHeaders.CONTENT_TYPE, "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body));
        applyToken(builder, token);
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private byte[] multipartBody(
            String boundary,
            String filename,
            String contentType,
            ProductImageType imageType,
            byte[] content
    ) {
        String head = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"imageType\"\r\n\r\n"
                + imageType.name() + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n";
        String tail = "\r\n--" + boundary + "--\r\n";
        byte[] headBytes = head.getBytes(StandardCharsets.UTF_8);
        byte[] tailBytes = tail.getBytes(StandardCharsets.UTF_8);
        byte[] body = new byte[headBytes.length + content.length + tailBytes.length];
        System.arraycopy(headBytes, 0, body, 0, headBytes.length);
        System.arraycopy(content, 0, body, headBytes.length, content.length);
        System.arraycopy(tailBytes, 0, body, headBytes.length + content.length, tailBytes.length);
        return body;
    }

    private HttpResponse<String> patch(String path, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri(path))
                .method("PATCH", HttpRequest.BodyPublishers.noBody());
        applyToken(builder, token);
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri(path))
                .GET();
        applyToken(builder, token);
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> putJson(String path, String json, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri(path))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .PUT(HttpRequest.BodyPublishers.ofString(json));
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

    private String contentTypeFor(String filename) {
        if (filename.endsWith(".png")) {
            return "image/png";
        }
        if (filename.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
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
