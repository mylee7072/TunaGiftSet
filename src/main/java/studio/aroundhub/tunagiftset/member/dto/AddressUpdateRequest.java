package studio.aroundhub.tunagiftset.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressUpdateRequest(
        @Size(max = 100, message = "Address name must be 100 characters or less.")
        String addressName,

        @NotBlank(message = "Recipient name is required.")
        @Size(max = 100, message = "Recipient name must be 100 characters or less.")
        String recipientName,

        @NotBlank(message = "Recipient phone is required.")
        @Pattern(regexp = "^01\\d-?\\d{3,4}-?\\d{4}$", message = "Recipient phone format is invalid.")
        String recipientPhone,

        @NotBlank(message = "Zip code is required.")
        @Pattern(regexp = "^\\d{5}$", message = "Zip code must be 5 digits.")
        String zipCode,

        @NotBlank(message = "Road address is required.")
        @Size(max = 255, message = "Road address must be 255 characters or less.")
        String roadAddress,

        @Size(max = 255, message = "Jibun address must be 255 characters or less.")
        String jibunAddress,

        @NotBlank(message = "Detail address is required.")
        @Size(max = 255, message = "Detail address must be 255 characters or less.")
        String detailAddress,

        @Size(max = 255, message = "Extra address must be 255 characters or less.")
        String extraAddress,

        Boolean defaultAddress
) {
}
