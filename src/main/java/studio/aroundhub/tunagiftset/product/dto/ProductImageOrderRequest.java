package studio.aroundhub.tunagiftset.product.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ProductImageOrderRequest(
        @NotEmpty(message = "이미지 ID 목록은 필수입니다.")
        List<Long> imageIds
) {
}
