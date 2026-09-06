package studio.aroundhub.tunagiftset.product.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import studio.aroundhub.tunagiftset.entity.type.ProductStatus;
import studio.aroundhub.tunagiftset.product.dto.ProductCreateRequest;
import studio.aroundhub.tunagiftset.product.dto.ProductPageResponse;
import studio.aroundhub.tunagiftset.product.dto.ProductResponse;
import studio.aroundhub.tunagiftset.product.dto.ProductSortType;
import studio.aroundhub.tunagiftset.product.dto.ProductStatusUpdateRequest;
import studio.aroundhub.tunagiftset.product.dto.ProductUpdateRequest;
import studio.aroundhub.tunagiftset.product.service.ProductService;

@RestController
@RequestMapping("/api")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // Gated to ROLE_ADMIN by SecurityConfig's /api/admin/** rule.
    @PostMapping("/admin/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductCreateRequest request) {
        return productService.create(request);
    }

    // Gated to ROLE_ADMIN by SecurityConfig's /api/admin/** rule.
    @PutMapping("/admin/products/{productId}")
    public ProductResponse update(
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        return productService.update(productId, request);
    }

    // Gated to ROLE_ADMIN by SecurityConfig's /api/admin/** rule.
    @PatchMapping("/admin/products/{productId}/status")
    public ProductResponse changeStatus(
            @PathVariable Long productId,
            @Valid @RequestBody ProductStatusUpdateRequest request
    ) {
        return productService.changeStatus(productId, request);
    }

    // Gated to ROLE_ADMIN — includes HIDDEN/DISCONTINUED products the storefront listing below excludes.
    @GetMapping("/admin/products")
    public ProductPageResponse findAdminProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(defaultValue = "LATEST") ProductSortType sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return productService.findAdminProducts(keyword, categoryId, brandId, status, featured, sort, page, size);
    }

    // Gated to ROLE_ADMIN — unlike findProduct below, does not hide HIDDEN/DISCONTINUED products.
    @GetMapping("/admin/products/{productId}")
    public ProductResponse findAdminProduct(@PathVariable Long productId) {
        return productService.findAdminProduct(productId);
    }

    @GetMapping("/products")
    public ProductPageResponse findProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(defaultValue = "LATEST") ProductSortType sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return productService.findProducts(keyword, categoryId, brandId, status, featured, sort, page, size);
    }

    @GetMapping("/products/{productId}")
    public ProductResponse findProduct(@PathVariable Long productId) {
        return productService.findProduct(productId);
    }
}
