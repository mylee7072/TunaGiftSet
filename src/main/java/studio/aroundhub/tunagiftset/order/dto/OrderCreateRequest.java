package studio.aroundhub.tunagiftset.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OrderCreateRequest(
        @NotEmpty(message = "At least one cart item must be selected.")
        List<Long> cartItemIds,

        Long memberCouponId,

        Long addressId,

        @Size(max = 100, message = "Recipient name must be 100 characters or less.")
        String recipientName,

        @Size(max = 30, message = "Recipient phone must be 30 characters or less.")
        String recipientPhone,

        @Size(max = 20, message = "Zip code must be 20 characters or less.")
        String zipCode,

        @Size(max = 255, message = "Road address must be 255 characters or less.")
        String roadAddress,

        @Size(max = 255, message = "Jibun address must be 255 characters or less.")
        String jibunAddress,

        @Size(max = 255, message = "Detail address must be 255 characters or less.")
        String detailAddress,

        @Size(max = 255, message = "Extra address must be 255 characters or less.")
        String extraAddress,

        @Size(max = 255, message = "Address1 must be 255 characters or less.")
        String address1,

        @Size(max = 255, message = "Address2 must be 255 characters or less.")
        String address2,

        @Size(max = 100, message = "Delivery message must be 100 characters or less.")
        String deliveryMessage
) {
    public OrderCreateRequest(
            List<Long> cartItemIds,
            String recipientName,
            String recipientPhone,
            String zipCode,
            String address1,
            String address2,
            String deliveryMessage
    ) {
        this(
                cartItemIds,
                null,
                null,
                recipientName,
                recipientPhone,
                zipCode,
                null,
                null,
                null,
                null,
                address1,
                address2,
                deliveryMessage
        );
    }
}
