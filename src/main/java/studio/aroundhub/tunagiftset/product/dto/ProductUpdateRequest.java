package studio.aroundhub.tunagiftset.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import studio.aroundhub.tunagiftset.entity.type.ProductStatus;

public record ProductUpdateRequest(
        @NotNull(message = "브랜드는 필수입니다.")
        @Positive(message = "브랜드 ID는 1 이상이어야 합니다.")
        Long brandId,

        @NotNull(message = "카테고리는 필수입니다.")
        @Positive(message = "카테고리 ID는 1 이상이어야 합니다.")
        Long categoryId,

        @NotBlank(message = "상품 코드는 필수입니다.")
        @Size(max = 50, message = "상품 코드는 50자 이하여야 합니다.")
        String productCode,

        @NotBlank(message = "상품명은 필수입니다.")
        @Size(max = 200, message = "상품명은 200자 이하여야 합니다.")
        String name,

        @Size(max = 500, message = "짧은 설명은 500자 이하여야 합니다.")
        String shortDescription,

        String description,

        @NotNull(message = "정상가격은 필수입니다.")
        @Min(value = 0, message = "정상가격은 0 이상이어야 합니다.")
        BigDecimal originalPrice,

        @NotNull(message = "판매가격은 필수입니다.")
        @Min(value = 0, message = "판매가격은 0 이상이어야 합니다.")
        BigDecimal salePrice,

        @Min(value = 0, message = "재고수량은 0 이상이어야 합니다.")
        int stockQuantity,

        @NotNull(message = "상품 상태는 필수입니다.")
        ProductStatus status,

        boolean featured
) {
}
