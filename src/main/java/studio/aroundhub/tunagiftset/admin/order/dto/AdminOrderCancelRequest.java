package studio.aroundhub.tunagiftset.admin.order.dto;

import jakarta.validation.constraints.Size;

public record AdminOrderCancelRequest(
        @Size(max = 200, message = "취소 사유는 200자 이하여야 합니다.")
        String reason
) {
}
