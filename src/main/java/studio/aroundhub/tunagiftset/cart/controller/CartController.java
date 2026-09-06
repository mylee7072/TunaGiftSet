package studio.aroundhub.tunagiftset.cart.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import studio.aroundhub.tunagiftset.cart.dto.CartItemAddRequest;
import studio.aroundhub.tunagiftset.cart.dto.CartItemDeleteRequest;
import studio.aroundhub.tunagiftset.cart.dto.CartItemQuantityUpdateRequest;
import studio.aroundhub.tunagiftset.cart.dto.CartResponse;
import studio.aroundhub.tunagiftset.cart.service.CartService;
import studio.aroundhub.tunagiftset.security.AuthMember;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse getCart(@AuthenticationPrincipal AuthMember authMember) {
        return cartService.getCart(authMember.id());
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartResponse addItem(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody CartItemAddRequest request
    ) {
        return cartService.addItem(authMember.id(), request);
    }

    @PatchMapping("/items/{cartItemId}")
    public CartResponse updateQuantity(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long cartItemId,
            @Valid @RequestBody CartItemQuantityUpdateRequest request
    ) {
        return cartService.updateQuantity(authMember.id(), cartItemId, request);
    }

    @DeleteMapping("/items/{cartItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long cartItemId
    ) {
        cartService.deleteItem(authMember.id(), cartItemId);
    }

    @DeleteMapping("/items")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItems(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody(required = false) CartItemDeleteRequest request
    ) {
        cartService.deleteItems(authMember.id(), request);
    }
}
