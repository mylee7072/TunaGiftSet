package studio.aroundhub.tunagiftset.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;
import studio.aroundhub.tunagiftset.entity.type.ProductImageType;

public record ProductImageUploadRequest(
        @NotNull(message = "이미지 파일은 필수입니다.")
        MultipartFile file,

        ProductImageType imageType,

        @Min(value = 0, message = "이미지 순서는 0 이상이어야 합니다.")
        Integer displayOrder
) {
}
