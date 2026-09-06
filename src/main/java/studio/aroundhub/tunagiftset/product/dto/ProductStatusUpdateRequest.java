package studio.aroundhub.tunagiftset.product.dto;

import jakarta.validation.constraints.NotNull;
import studio.aroundhub.tunagiftset.entity.type.ProductStatus;

public record ProductStatusUpdateRequest(
        @NotNull(message = "상품 상태는 필수입니다.")
        ProductStatus status
) {
}
