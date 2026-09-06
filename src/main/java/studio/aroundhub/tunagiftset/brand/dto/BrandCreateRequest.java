package studio.aroundhub.tunagiftset.brand.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BrandCreateRequest(
        @NotBlank(message = "브랜드 코드는 필수입니다.")
        @Size(max = 100, message = "브랜드 코드는 100자 이하여야 합니다.")
        String name,

        @NotBlank(message = "브랜드 표시명은 필수입니다.")
        @Size(max = 100, message = "브랜드 표시명은 100자 이하여야 합니다.")
        String displayName,

        boolean active
) {
}
