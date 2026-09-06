package studio.aroundhub.tunagiftset.member.dto;

import java.time.Instant;
import studio.aroundhub.tunagiftset.entity.Address;

public record AddressResponse(
        Long id,
        String addressName,
        String recipientName,
        String recipientPhone,
        String zipCode,
        String roadAddress,
        String jibunAddress,
        String detailAddress,
        String extraAddress,
        boolean defaultAddress,
        Instant createdAt,
        Instant updatedAt
) {
    public static AddressResponse from(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getAddressName(),
                address.getRecipientName(),
                address.getRecipientPhone(),
                address.getZipCode(),
                address.getRoadAddress(),
                address.getJibunAddress(),
                address.getDetailAddress(),
                address.getExtraAddress(),
                address.isDefaultAddress(),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }
}
