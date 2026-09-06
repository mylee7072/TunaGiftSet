package studio.aroundhub.tunagiftset.admin.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminDeliveryUpdateRequest(
        @NotBlank(message = "택배사는 필수입니다.")
        @Size(max = 100, message = "택배사명은 100자 이하여야 합니다.")
        String carrier,

        @NotBlank(message = "송장번호는 필수입니다.")
        @Size(max = 100, message = "송장번호는 100자 이하여야 합니다.")
        @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "송장번호는 영문, 숫자, '-'만 사용할 수 있습니다.")
        String trackingNumber
) {
}
