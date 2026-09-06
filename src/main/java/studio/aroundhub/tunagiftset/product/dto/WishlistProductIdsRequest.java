package studio.aroundhub.tunagiftset.product.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record WishlistProductIdsRequest(
        @NotNull
        @Size(max = 100)
        List<Long> productIds
) {
}
