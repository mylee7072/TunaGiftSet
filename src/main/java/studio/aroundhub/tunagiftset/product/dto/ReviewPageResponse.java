package studio.aroundhub.tunagiftset.product.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record ReviewPageResponse(
        List<ReviewResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static ReviewPageResponse from(Page<ReviewResponse> page) {
        return new ReviewPageResponse(
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
