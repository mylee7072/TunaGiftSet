package studio.aroundhub.tunagiftset.product.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tools.jackson.databind.ObjectMapper;
import studio.aroundhub.tunagiftset.entity.type.ProductStatus;
import studio.aroundhub.tunagiftset.exception.GlobalExceptionHandler;
import studio.aroundhub.tunagiftset.exception.ResourceNotFoundException;
import studio.aroundhub.tunagiftset.product.dto.ProductCreateRequest;
import studio.aroundhub.tunagiftset.product.dto.ProductPageResponse;
import studio.aroundhub.tunagiftset.product.dto.ProductResponse;
import studio.aroundhub.tunagiftset.product.dto.ProductStatusUpdateRequest;
import studio.aroundhub.tunagiftset.product.dto.ProductUpdateRequest;
import studio.aroundhub.tunagiftset.product.service.ProductService;

class ProductControllerTest {

    private final ProductService productService = org.mockito.Mockito.mock(ProductService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(productService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createProduct() throws Exception {
        when(productService.create(any())).thenReturn(response(ProductStatus.ACTIVE));

        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest(BigDecimal.valueOf(50000), 100))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productCode").value("SAMPLE-001"))
                .andExpect(jsonPath("$.name").value("SAMPLE GIFT SET"));
    }

    @Test
    void createProductFailsWhenPriceIsNegative() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest(BigDecimal.valueOf(-1), 100))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createProductFailsWhenStockIsNegative() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest(BigDecimal.valueOf(50000), -1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void updateProduct() throws Exception {
        when(productService.update(any(), any())).thenReturn(response(ProductStatus.SOLD_OUT));

        mockMvc.perform(put("/api/admin/products/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLD_OUT"));
    }

    @Test
    void changeProductStatus() throws Exception {
        when(productService.changeStatus(any(), any())).thenReturn(response(ProductStatus.HIDDEN));

        mockMvc.perform(patch("/api/admin/products/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProductStatusUpdateRequest(ProductStatus.HIDDEN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HIDDEN"));
    }

    @Test
    void findProducts() throws Exception {
        when(productService.findProducts(any(), any(), any(), any(), any(), any(), any(Integer.class), any(Integer.class)))
                .thenReturn(new ProductPageResponse(List.of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void findProduct() throws Exception {
        when(productService.findProduct(10L)).thenReturn(response(ProductStatus.ACTIVE));

        mockMvc.perform(get("/api/products/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void findProductFailsWhenProductDoesNotExist() throws Exception {
        when(productService.findProduct(999L))
                .thenThrow(new ResourceNotFoundException("PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다."));

        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    private ProductCreateRequest createRequest(BigDecimal originalPrice, int stockQuantity) {
        return new ProductCreateRequest(
                1L,
                1L,
                "SAMPLE-001",
                "SAMPLE GIFT SET",
                "테스트용 SAMPLE 상품입니다.",
                "실제 판매 상품이 아닌 SAMPLE 데이터입니다.",
                originalPrice,
                BigDecimal.valueOf(45000),
                stockQuantity,
                ProductStatus.ACTIVE,
                true
        );
    }

    private ProductUpdateRequest updateRequest() {
        return new ProductUpdateRequest(
                1L,
                1L,
                "SAMPLE-001",
                "SAMPLE GIFT SET",
                "테스트용 SAMPLE 상품입니다.",
                "실제 판매 상품이 아닌 SAMPLE 데이터입니다.",
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(45000),
                100,
                ProductStatus.SOLD_OUT,
                true
        );
    }

    private ProductResponse response(ProductStatus status) {
        return new ProductResponse(
                10L,
                "SAMPLE-001",
                "SAMPLE GIFT SET",
                1L,
                "SAMPLE",
                "SAMPLE BRAND",
                1L,
                "SAMPLE CATEGORY",
                "테스트용 SAMPLE 상품입니다.",
                "실제 판매 상품이 아닌 SAMPLE 데이터입니다.",
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(45000),
                10,
                100,
                false,
                status,
                true,
                BigDecimal.ZERO,
                0,
                0,
                List.of()
        );
    }
}
