package studio.aroundhub.tunagiftset.admin.inventory.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record InventoryHistoryPageResponse(
        List<InventoryHistoryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static InventoryHistoryPageResponse from(Page<InventoryHistoryResponse> page) {
        return new InventoryHistoryPageResponse(
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
