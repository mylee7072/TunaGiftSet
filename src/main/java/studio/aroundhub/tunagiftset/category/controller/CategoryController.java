package studio.aroundhub.tunagiftset.category.controller;

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
import studio.aroundhub.tunagiftset.category.dto.CategoryCreateRequest;
import studio.aroundhub.tunagiftset.category.dto.CategoryResponse;
import studio.aroundhub.tunagiftset.category.dto.CategoryUpdateRequest;
import studio.aroundhub.tunagiftset.category.service.CategoryService;

@RestController
@RequestMapping("/api")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // Gated to ROLE_ADMIN by SecurityConfig's /api/admin/** rule.
    @PostMapping("/admin/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryCreateRequest request) {
        return categoryService.create(request);
    }

    // Gated to ROLE_ADMIN by SecurityConfig's /api/admin/** rule.
    @PutMapping("/admin/categories/{categoryId}")
    public CategoryResponse update(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryUpdateRequest request
    ) {
        return categoryService.update(categoryId, request);
    }

    // Gated to ROLE_ADMIN — includes inactive categories the public listing below hides.
    @GetMapping("/admin/categories")
    public List<CategoryResponse> findAllCategories() {
        return categoryService.findAllCategories();
    }

    @GetMapping("/categories")
    public List<CategoryResponse> findActiveCategories() {
        return categoryService.findActiveCategories();
    }
}
