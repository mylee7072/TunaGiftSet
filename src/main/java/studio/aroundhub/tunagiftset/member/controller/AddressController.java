package studio.aroundhub.tunagiftset.member.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import studio.aroundhub.tunagiftset.member.dto.AddressCreateRequest;
import studio.aroundhub.tunagiftset.member.dto.AddressResponse;
import studio.aroundhub.tunagiftset.member.dto.AddressUpdateRequest;
import studio.aroundhub.tunagiftset.member.service.AddressService;
import studio.aroundhub.tunagiftset.security.AuthMember;

@RestController
@RequestMapping("/api/members/me/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public List<AddressResponse> findAddresses(@AuthenticationPrincipal AuthMember authMember) {
        return addressService.findAddresses(authMember.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse create(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody AddressCreateRequest request
    ) {
        return addressService.create(authMember.id(), request);
    }

    @PutMapping("/{addressId}")
    public AddressResponse update(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long addressId,
            @Valid @RequestBody AddressUpdateRequest request
    ) {
        return addressService.update(authMember.id(), addressId, request);
    }

    @PatchMapping("/{addressId}/default")
    public AddressResponse setDefault(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long addressId
    ) {
        return addressService.setDefault(authMember.id(), addressId);
    }

    @DeleteMapping("/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long addressId
    ) {
        addressService.delete(authMember.id(), addressId);
    }
}
