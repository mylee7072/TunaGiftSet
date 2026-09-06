package studio.aroundhub.tunagiftset.product.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import studio.aroundhub.tunagiftset.product.dto.ProductImageOrderRequest;
import studio.aroundhub.tunagiftset.product.dto.ProductImageResponse;
import studio.aroundhub.tunagiftset.product.dto.ProductImageUploadRequest;
import studio.aroundhub.tunagiftset.product.service.ProductImageService;

@RestController
@RequestMapping("/api/admin/products/{productId}/images")
public class ProductImageAdminController {

    private final ProductImageService productImageService;

    public ProductImageAdminController(ProductImageService productImageService) {
        this.productImageService = productImageService;
    }

    @GetMapping
    public List<ProductImageResponse> findImages(@PathVariable Long productId) {
        return productImageService.findImages(productId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductImageResponse upload(
            @PathVariable Long productId,
            @Valid @ModelAttribute ProductImageUploadRequest request
    ) {
        return productImageService.upload(productId, request);
    }

    @PatchMapping("/{imageId}/main")
    public ProductImageResponse setMain(
            @PathVariable Long productId,
            @PathVariable Long imageId
    ) {
        return productImageService.setMain(productId, imageId);
    }

    @PutMapping("/order")
    public List<ProductImageResponse> reorder(
            @PathVariable Long productId,
            @Valid @RequestBody ProductImageOrderRequest request
    ) {
        return productImageService.reorder(productId, request);
    }

    @DeleteMapping("/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long productId,
            @PathVariable Long imageId
    ) {
        productImageService.delete(productId, imageId);
    }
}
