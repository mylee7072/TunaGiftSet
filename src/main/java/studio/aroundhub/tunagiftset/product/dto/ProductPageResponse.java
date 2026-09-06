package studio.aroundhub.tunagiftset.product.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record ProductPageResponse(
        List<ProductListResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static ProductPageResponse from(Page<ProductListResponse> page) {
        return new ProductPageResponse(
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
