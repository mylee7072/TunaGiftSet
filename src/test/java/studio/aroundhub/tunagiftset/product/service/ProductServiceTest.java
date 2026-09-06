package studio.aroundhub.tunagiftset.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import studio.aroundhub.tunagiftset.brand.service.BrandService;
import studio.aroundhub.tunagiftset.category.service.CategoryService;
import studio.aroundhub.tunagiftset.entity.Brand;
import studio.aroundhub.tunagiftset.entity.Category;
import studio.aroundhub.tunagiftset.entity.Product;
import studio.aroundhub.tunagiftset.entity.type.ProductStatus;
import studio.aroundhub.tunagiftset.exception.DuplicateResourceException;
import studio.aroundhub.tunagiftset.exception.ResourceNotFoundException;
import studio.aroundhub.tunagiftset.product.dto.ProductCreateRequest;
import studio.aroundhub.tunagiftset.product.dto.ProductPageResponse;
import studio.aroundhub.tunagiftset.product.dto.ProductResponse;
import studio.aroundhub.tunagiftset.product.dto.ProductSortType;
import studio.aroundhub.tunagiftset.product.dto.ProductStatusUpdateRequest;
import studio.aroundhub.tunagiftset.product.dto.ProductUpdateRequest;
import studio.aroundhub.tunagiftset.repository.ProductImageRepository;
import studio.aroundhub.tunagiftset.repository.ProductRepository;
import studio.aroundhub.tunagiftset.storage.ObjectStorageService;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private BrandService brandService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private ObjectStorageService objectStorageService;

    @Mock
    private ReviewService reviewService;

    @Mock
    private WishlistService wishlistService;

    @InjectMocks
    private ProductService productService;

    private Brand brand;
    private Category category;

    @BeforeEach
    void setUp() {
        brand = new Brand("SAMPLE", "SAMPLE BRAND", true);
        category = new Category(null, "SAMPLE CATEGORY", 1, true);
        ReflectionTestUtils.setField(brand, "id", 1L);
        ReflectionTestUtils.setField(category, "id", 1L);
        lenient().when(reviewService.summarize(any(Long.class)))
                .thenReturn(new studio.aroundhub.tunagiftset.product.dto.ReviewSummaryResponse(BigDecimal.ZERO, 0));
        lenient().when(reviewService.summarize(any(List.class))).thenReturn(java.util.Map.of());
        lenient().when(wishlistService.countByProductIds(any(List.class))).thenReturn(java.util.Map.of());
    }

    @Test
    void createProduct() {
        ProductCreateRequest request = createRequest("SAMPLE-001");
        when(brandService.getBrand(1L)).thenReturn(brand);
        when(categoryService.getCategory(1L)).thenReturn(category);
        when(productRepository.existsByProductCode("SAMPLE-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            ReflectionTestUtils.setField(product, "id", 10L);
            return product;
        });

        ProductResponse response = productService.create(request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.productCode()).isEqualTo("SAMPLE-001");
        assertThat(response.name()).isEqualTo("SAMPLE GIFT SET");

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getSalePrice()).isEqualByComparingTo("45000");
    }

    @Test
    void createProductFailsWhenBrandDoesNotExist() {
        ProductCreateRequest request = createRequest("SAMPLE-001");
        when(brandService.getBrand(1L))
                .thenThrow(new ResourceNotFoundException("BRAND_NOT_FOUND", "브랜드를 찾을 수 없습니다."));

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("브랜드를 찾을 수 없습니다.");

        verify(productRepository, never()).save(any());
    }

    @Test
    void createProductFailsWhenCategoryDoesNotExist() {
        ProductCreateRequest request = createRequest("SAMPLE-001");
        when(brandService.getBrand(1L)).thenReturn(brand);
        when(categoryService.getCategory(1L))
                .thenThrow(new ResourceNotFoundException("CATEGORY_NOT_FOUND", "카테고리를 찾을 수 없습니다."));

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("카테고리를 찾을 수 없습니다.");

        verify(productRepository, never()).save(any());
    }

    @Test
    void createProductFailsWhenProductCodeDuplicated() {
        ProductCreateRequest request = createRequest("SAMPLE-001");
        when(brandService.getBrand(1L)).thenReturn(brand);
        when(categoryService.getCategory(1L)).thenReturn(category);
        when(productRepository.existsByProductCode("SAMPLE-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("이미 사용 중인 상품 코드입니다.");

        verify(productRepository, never()).save(any());
    }

    @Test
    void updateProduct() {
        Product product = product("SAMPLE-001", ProductStatus.ACTIVE);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(brandService.getBrand(1L)).thenReturn(brand);
        when(categoryService.getCategory(1L)).thenReturn(category);
        when(productRepository.existsByProductCodeAndIdNot("SAMPLE-002", 10L)).thenReturn(false);
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(10L)).thenReturn(List.of());

        ProductResponse response = productService.update(10L, updateRequest("SAMPLE-002"));

        assertThat(response.productCode()).isEqualTo("SAMPLE-002");
        assertThat(response.name()).isEqualTo("UPDATED SAMPLE GIFT SET");
        assertThat(response.stockQuantity()).isEqualTo(50);
    }

    @Test
    void changeProductStatus() {
        Product product = product("SAMPLE-001", ProductStatus.ACTIVE);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(10L)).thenReturn(List.of());

        ProductResponse response = productService.changeStatus(10L, new ProductStatusUpdateRequest(ProductStatus.HIDDEN));

        assertThat(response.status()).isEqualTo(ProductStatus.HIDDEN);
    }

    @Test
    void findProducts() {
        Product product = product("SAMPLE-001", ProductStatus.ACTIVE);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));

        ProductPageResponse response = productService.findProducts(
                "sample",
                1L,
                1L,
                ProductStatus.ACTIVE,
                true,
                ProductSortType.LATEST,
                0,
                20
        );

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).productCode()).isEqualTo("SAMPLE-001");
    }

    @Test
    void findProduct() {
        Product product = product("SAMPLE-001", ProductStatus.ACTIVE);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(10L)).thenReturn(List.of());

        ProductResponse response = productService.findProduct(10L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.discountRate()).isEqualTo(10);
    }

    @Test
    void findProductFailsWhenProductDoesNotExist() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findProduct(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("상품을 찾을 수 없습니다.");
    }

    private ProductCreateRequest createRequest(String productCode) {
        return new ProductCreateRequest(
                1L,
                1L,
                productCode,
                "SAMPLE GIFT SET",
                "테스트용 SAMPLE 상품입니다.",
                "실제 판매 상품이 아닌 SAMPLE 데이터입니다.",
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(45000),
                100,
                ProductStatus.ACTIVE,
                true
        );
    }

    private ProductUpdateRequest updateRequest(String productCode) {
        return new ProductUpdateRequest(
                1L,
                1L,
                productCode,
                "UPDATED SAMPLE GIFT SET",
                "수정된 SAMPLE 상품입니다.",
                "실제 판매 상품이 아닌 수정 테스트용 SAMPLE 데이터입니다.",
                BigDecimal.valueOf(60000),
                BigDecimal.valueOf(54000),
                50,
                ProductStatus.SOLD_OUT,
                false
        );
    }

    private Product product(String productCode, ProductStatus status) {
        Product product = new Product(
                brand,
                category,
                productCode,
                "SAMPLE GIFT SET",
                "테스트용 SAMPLE 상품입니다.",
                "실제 판매 상품이 아닌 SAMPLE 데이터입니다.",
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(45000),
                100,
                status,
                true
        );
        ReflectionTestUtils.setField(product, "id", 10L);
        return product;
    }
}
