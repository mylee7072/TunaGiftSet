package studio.aroundhub.tunagiftset.category.dto;

import studio.aroundhub.tunagiftset.entity.Category;

public record CategoryResponse(
        Long id,
        Long parentId,
        String name,
        int displayOrder,
        boolean active
) {
    public static CategoryResponse from(Category category) {
        Long parentId = category.getParent() == null ? null : category.getParent().getId();

        return new CategoryResponse(
                category.getId(),
                parentId,
                category.getName(),
                category.getDisplayOrder(),
                category.isActive()
        );
    }
}
