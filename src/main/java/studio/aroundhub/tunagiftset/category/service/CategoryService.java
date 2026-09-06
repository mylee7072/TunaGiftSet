package studio.aroundhub.tunagiftset.category.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studio.aroundhub.tunagiftset.category.dto.CategoryCreateRequest;
import studio.aroundhub.tunagiftset.category.dto.CategoryResponse;
import studio.aroundhub.tunagiftset.category.dto.CategoryUpdateRequest;
import studio.aroundhub.tunagiftset.entity.Category;
import studio.aroundhub.tunagiftset.exception.InvalidRequestException;
import studio.aroundhub.tunagiftset.exception.ResourceNotFoundException;
import studio.aroundhub.tunagiftset.repository.CategoryRepository;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public CategoryResponse create(CategoryCreateRequest request) {
        Category parent = resolveParent(request.parentId());
        Category category = new Category(parent, request.name(), request.displayOrder(), request.active());

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long categoryId, CategoryUpdateRequest request) {
        Category category = getCategory(categoryId);
        Category parent = resolveParent(request.parentId());

        if (parent != null && parent.getId().equals(categoryId)) {
            throw new InvalidRequestException("INVALID_CATEGORY_PARENT", "자기 자신을 상위 카테고리로 지정할 수 없습니다.");
        }

        category.update(parent, request.name(), request.displayOrder(), request.active());
        return CategoryResponse.from(category);
    }

    public List<CategoryResponse> findActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    /** Admin management view — includes inactive categories, which the public listing hides. */
    public List<CategoryResponse> findAllCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("CATEGORY_NOT_FOUND", "카테고리를 찾을 수 없습니다."));
    }

    private Category resolveParent(Long parentId) {
        if (parentId == null) {
            return null;
        }

        return getCategory(parentId);
    }
}
