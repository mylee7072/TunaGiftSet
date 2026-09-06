package studio.aroundhub.tunagiftset.cart.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CartItemDeleteRequest(
        @NotEmpty(message = "삭제할 장바구니 상품을 선택해야 합니다.")
        List<Long> cartItemIds
) {
}
