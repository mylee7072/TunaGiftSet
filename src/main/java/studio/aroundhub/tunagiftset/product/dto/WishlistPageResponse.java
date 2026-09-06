package studio.aroundhub.tunagiftset.product.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record WishlistPageResponse(
        List<WishlistItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static WishlistPageResponse from(Page<WishlistItemResponse> page) {
        return new WishlistPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
