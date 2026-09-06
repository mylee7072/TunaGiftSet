package studio.aroundhub.tunagiftset.admin.order.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record AdminOrderPageResponse(
        List<AdminOrderListResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static AdminOrderPageResponse from(Page<AdminOrderListResponse> page) {
        return new AdminOrderPageResponse(
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
