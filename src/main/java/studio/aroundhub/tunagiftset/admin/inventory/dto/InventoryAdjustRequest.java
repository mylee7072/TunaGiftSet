package studio.aroundhub.tunagiftset.admin.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

// type + a positive magnitude, rather than a single signed integer — a stray minus sign on a
// signed-quantity field is an easy, high-consequence typo for an operator to make.
public record InventoryAdjustRequest(
        @NotNull(message = "조정 유형은 필수입니다.")
        InventoryAdjustType type,

        @Positive(message = "수량은 1 이상이어야 합니다.")
        int quantity,

        @NotBlank(message = "조정 사유는 필수입니다.")
        @Size(max = 200, message = "조정 사유는 200자 이하여야 합니다.")
        String reason
) {
}
