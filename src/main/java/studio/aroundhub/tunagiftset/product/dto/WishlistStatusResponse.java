package studio.aroundhub.tunagiftset.product.dto;

public record WishlistStatusResponse(
        Long productId,
        boolean wishlisted,
        long wishlistCount
) {
}
