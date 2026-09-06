package studio.aroundhub.tunagiftset.admin.inventory.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import studio.aroundhub.tunagiftset.admin.inventory.dto.InventoryAdjustRequest;
import studio.aroundhub.tunagiftset.admin.inventory.dto.InventoryAdjustResponse;
import studio.aroundhub.tunagiftset.admin.inventory.dto.InventoryHistoryPageResponse;
import studio.aroundhub.tunagiftset.admin.inventory.dto.InventoryResponse;
import studio.aroundhub.tunagiftset.admin.inventory.service.InventoryService;
import studio.aroundhub.tunagiftset.security.AuthMember;

/** Every endpoint here is gated to ROLE_ADMIN by SecurityConfig's /api/admin/** rule. */
@RestController
@RequestMapping("/api/admin/products/{productId}/inventory")
public class AdminInventoryController {

    private final InventoryService inventoryService;

    public AdminInventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public InventoryResponse getInventory(@PathVariable Long productId) {
        return inventoryService.getInventory(productId);
    }

    @PostMapping("/adjust")
    public InventoryAdjustResponse adjust(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long productId,
            @Valid @RequestBody InventoryAdjustRequest request
    ) {
        return inventoryService.adjust(productId, request, authMember.id());
    }

    @GetMapping("/history")
    public InventoryHistoryPageResponse getHistory(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return inventoryService.getHistory(productId, page, size);
    }
}
