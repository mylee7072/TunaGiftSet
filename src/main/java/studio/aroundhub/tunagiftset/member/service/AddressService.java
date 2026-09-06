package studio.aroundhub.tunagiftset.member.service;

import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studio.aroundhub.tunagiftset.entity.Address;
import studio.aroundhub.tunagiftset.entity.Member;
import studio.aroundhub.tunagiftset.exception.InvalidRequestException;
import studio.aroundhub.tunagiftset.exception.ResourceNotFoundException;
import studio.aroundhub.tunagiftset.member.dto.AddressCreateRequest;
import studio.aroundhub.tunagiftset.member.dto.AddressResponse;
import studio.aroundhub.tunagiftset.member.dto.AddressUpdateRequest;
import studio.aroundhub.tunagiftset.repository.AddressRepository;
import studio.aroundhub.tunagiftset.repository.MemberRepository;

@Service
@Transactional(readOnly = true)
public class AddressService {

    private final AddressRepository addressRepository;
    private final MemberRepository memberRepository;
    private final int maxAddressesPerMember;

    public AddressService(
            AddressRepository addressRepository,
            MemberRepository memberRepository,
            @Value("${app.address.max-addresses-per-member:20}") int maxAddressesPerMember
    ) {
        this.addressRepository = addressRepository;
        this.memberRepository = memberRepository;
        this.maxAddressesPerMember = maxAddressesPerMember;
    }

    public List<AddressResponse> findAddresses(Long memberId) {
        return addressRepository.findAllByMemberIdOrderByDefaultAddressDescCreatedAtDesc(memberId).stream()
                .map(AddressResponse::from)
                .toList();
    }

    @Transactional
    public AddressResponse create(Long memberId, AddressCreateRequest request) {
        Member member = getMember(memberId);
        List<Address> addresses = addressRepository.findAllByMemberIdForUpdate(memberId);
        if (addresses.size() >= maxAddressesPerMember) {
            throw new InvalidRequestException("ADDRESS_LIMIT_EXCEEDED", "Address limit has been exceeded.");
        }

        boolean shouldBeDefault = addresses.isEmpty() || Boolean.TRUE.equals(request.defaultAddress());
        if (shouldBeDefault) {
            unmarkAll(addresses);
        }

        Address address = new Address(
                member,
                normalizeNullable(request.addressName()),
                normalizeRequired(request.recipientName(), "INVALID_ADDRESS", "Recipient name is required."),
                normalizePhone(request.recipientPhone()),
                normalizeZipCode(request.zipCode()),
                normalizeRequired(request.roadAddress(), "INVALID_ADDRESS", "Road address is required."),
                normalizeNullable(request.jibunAddress()),
                normalizeRequired(request.detailAddress(), "INVALID_ADDRESS", "Detail address is required."),
                normalizeNullable(request.extraAddress()),
                shouldBeDefault
        );
        return AddressResponse.from(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse update(Long memberId, Long addressId, AddressUpdateRequest request) {
        List<Address> addresses = addressRepository.findAllByMemberIdForUpdate(memberId);
        Address address = findInLockedAddresses(addresses, addressId);

        address.update(
                normalizeNullable(request.addressName()),
                normalizeRequired(request.recipientName(), "INVALID_ADDRESS", "Recipient name is required."),
                normalizePhone(request.recipientPhone()),
                normalizeZipCode(request.zipCode()),
                normalizeRequired(request.roadAddress(), "INVALID_ADDRESS", "Road address is required."),
                normalizeNullable(request.jibunAddress()),
                normalizeRequired(request.detailAddress(), "INVALID_ADDRESS", "Detail address is required."),
                normalizeNullable(request.extraAddress())
        );

        if (Boolean.TRUE.equals(request.defaultAddress())) {
            unmarkAll(addresses);
            address.markDefault();
        }

        return AddressResponse.from(address);
    }

    @Transactional
    public void delete(Long memberId, Long addressId) {
        List<Address> addresses = addressRepository.findAllByMemberIdForUpdate(memberId);
        Address address = findInLockedAddresses(addresses, addressId);
        boolean wasDefault = address.isDefaultAddress();

        addressRepository.delete(address);
        addresses.remove(address);

        if (wasDefault && !addresses.isEmpty()) {
            addresses.stream()
                    .max(Comparator.comparing(Address::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                            .thenComparing(Address::getId))
                    .orElseThrow()
                    .markDefault();
        }
    }

    @Transactional
    public AddressResponse setDefault(Long memberId, Long addressId) {
        List<Address> addresses = addressRepository.findAllByMemberIdForUpdate(memberId);
        Address address = findInLockedAddresses(addresses, addressId);
        unmarkAll(addresses);
        address.markDefault();
        return AddressResponse.from(address);
    }

    public Address findOwnedAddress(Long memberId, Long addressId) {
        return addressRepository.findByIdAndMemberId(addressId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("ADDRESS_NOT_FOUND", "Address was not found."));
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("MEMBER_NOT_FOUND", "Member was not found."));
    }

    private Address findInLockedAddresses(List<Address> addresses, Long addressId) {
        return addresses.stream()
                .filter(address -> address.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("ADDRESS_NOT_FOUND", "Address was not found."));
    }

    private void unmarkAll(List<Address> addresses) {
        addresses.forEach(Address::unmarkDefault);
    }

    private String normalizePhone(String phone) {
        String normalized = normalizeRequired(phone, "INVALID_PHONE_NUMBER", "Recipient phone is required.")
                .replaceAll("\\D", "");
        if (!normalized.matches("^01\\d{8,9}$")) {
            throw new InvalidRequestException("INVALID_PHONE_NUMBER", "Recipient phone format is invalid.");
        }
        return normalized;
    }

    private String normalizeZipCode(String zipCode) {
        String normalized = normalizeRequired(zipCode, "INVALID_ZIP_CODE", "Zip code is required.");
        if (!normalized.matches("^\\d{5}$")) {
            throw new InvalidRequestException("INVALID_ZIP_CODE", "Zip code must be 5 digits.");
        }
        return normalized;
    }

    private String normalizeRequired(String value, String code, String message) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new InvalidRequestException(code, message);
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
