package studio.aroundhub.tunagiftset.category.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequest(
        Long parentId,

        @NotBlank(message = "카테고리명은 필수입니다.")
        @Size(max = 100, message = "카테고리명은 100자 이하여야 합니다.")
        String name,

        @Min(value = 0, message = "표시 순서는 0 이상이어야 합니다.")
        int displayOrder,

        boolean active
) {
}
