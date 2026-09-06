package studio.aroundhub.tunagiftset.admin.inventory.dto;

import java.time.Instant;
import studio.aroundhub.tunagiftset.entity.InventoryHistory;
import studio.aroundhub.tunagiftset.entity.type.InventoryChangeType;

public record InventoryHistoryResponse(
        InventoryChangeType type,
        int quantityBefore,
        int changeQuantity,
        int quantityAfter,
        String reason,
        Long adminMemberId,
        Instant createdAt
) {
    public static InventoryHistoryResponse from(InventoryHistory history) {
        return new InventoryHistoryResponse(
                history.getChangeType(),
                history.getQuantityBefore(),
                history.getChangeQuantity(),
                history.getQuantityAfter(),
                history.getReason(),
                history.getAdminMemberId(),
                history.getCreatedAt()
        );
    }
}
