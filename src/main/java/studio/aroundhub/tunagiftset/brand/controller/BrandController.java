package studio.aroundhub.tunagiftset.brand.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import studio.aroundhub.tunagiftset.brand.dto.BrandCreateRequest;
import studio.aroundhub.tunagiftset.brand.dto.BrandResponse;
import studio.aroundhub.tunagiftset.brand.dto.BrandUpdateRequest;
import studio.aroundhub.tunagiftset.brand.service.BrandService;

@RestController
@RequestMapping("/api")
public class BrandController {

    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    // Gated to ROLE_ADMIN by SecurityConfig's /api/admin/** rule.
    @PostMapping("/admin/brands")
    @ResponseStatus(HttpStatus.CREATED)
    public BrandResponse create(@Valid @RequestBody BrandCreateRequest request) {
        return brandService.create(request);
    }

    // Gated to ROLE_ADMIN by SecurityConfig's /api/admin/** rule.
    @PutMapping("/admin/brands/{brandId}")
    public BrandResponse update(
            @PathVariable Long brandId,
            @Valid @RequestBody BrandUpdateRequest request
    ) {
        return brandService.update(brandId, request);
    }

    // Gated to ROLE_ADMIN — includes inactive brands the public listing below hides.
    @GetMapping("/admin/brands")
    public List<BrandResponse> findAllBrands() {
        return brandService.findAllBrands();
    }

    @GetMapping("/brands")
    public List<BrandResponse> findActiveBrands() {
        return brandService.findActiveBrands();
    }
}
