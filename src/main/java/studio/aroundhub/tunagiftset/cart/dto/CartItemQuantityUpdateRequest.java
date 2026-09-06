package studio.aroundhub.tunagiftset.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CartItemQuantityUpdateRequest(
        @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
        @Max(value = 99, message = "수량은 99 이하여야 합니다.")
        int quantity
) {
}
