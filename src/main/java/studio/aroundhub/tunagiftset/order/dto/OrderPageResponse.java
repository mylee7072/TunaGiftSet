package studio.aroundhub.tunagiftset.order.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record OrderPageResponse(
        List<OrderListResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static OrderPageResponse from(Page<OrderListResponse> page) {
        return new OrderPageResponse(
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
