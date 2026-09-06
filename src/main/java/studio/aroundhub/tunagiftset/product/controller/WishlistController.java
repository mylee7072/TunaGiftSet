package studio.aroundhub.tunagiftset.product.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import studio.aroundhub.tunagiftset.product.dto.WishlistPageResponse;
import studio.aroundhub.tunagiftset.product.dto.WishlistProductIdsRequest;
import studio.aroundhub.tunagiftset.product.dto.WishlistProductIdsResponse;
import studio.aroundhub.tunagiftset.product.dto.WishlistStatusResponse;
import studio.aroundhub.tunagiftset.product.service.WishlistService;
import studio.aroundhub.tunagiftset.security.AuthMember;

@RestController
@RequestMapping("/api")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @PostMapping("/products/{productId}/wishlist")
    public WishlistStatusResponse add(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long productId
    ) {
        return wishlistService.add(authMember.id(), productId);
    }

    @DeleteMapping("/products/{productId}/wishlist")
    public WishlistStatusResponse remove(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long productId
    ) {
        return wishlistService.remove(authMember.id(), productId);
    }

    @GetMapping("/members/me/wishlist")
    public WishlistPageResponse findMyWishlist(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return wishlistService.findMyWishlist(authMember.id(), page, size);
    }

    @GetMapping("/members/me/wishlist/products/{productId}/status")
    public WishlistStatusResponse status(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long productId
    ) {
        return wishlistService.status(authMember.id(), productId);
    }

    @PostMapping("/members/me/wishlist/product-ids")
    public WishlistProductIdsResponse findWishlistedProductIds(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody WishlistProductIdsRequest request
    ) {
        return wishlistService.findWishlistedProductIds(authMember.id(), request.productIds());
    }
}
