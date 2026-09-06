package studio.aroundhub.tunagiftset.admin.inventory.service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studio.aroundhub.tunagiftset.admin.inventory.dto.InventoryAdjustRequest;
import studio.aroundhub.tunagiftset.admin.inventory.dto.InventoryAdjustResponse;
import studio.aroundhub.tunagiftset.admin.inventory.dto.InventoryAdjustType;
import studio.aroundhub.tunagiftset.admin.inventory.dto.InventoryHistoryPageResponse;
import studio.aroundhub.tunagiftset.admin.inventory.dto.InventoryHistoryResponse;
import studio.aroundhub.tunagiftset.admin.inventory.dto.InventoryResponse;
import studio.aroundhub.tunagiftset.entity.InventoryHistory;
import studio.aroundhub.tunagiftset.entity.Product;
import studio.aroundhub.tunagiftset.entity.type.InventoryChangeType;
import studio.aroundhub.tunagiftset.exception.InvalidRequestException;
import studio.aroundhub.tunagiftset.exception.ResourceNotFoundException;
import studio.aroundhub.tunagiftset.repository.InventoryHistoryRepository;
import studio.aroundhub.tunagiftset.repository.ProductRepository;

@Service
@Transactional(readOnly = true)
public class InventoryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProductRepository productRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;

    public InventoryService(ProductRepository productRepository, InventoryHistoryRepository inventoryHistoryRepository) {
        this.productRepository = productRepository;
        this.inventoryHistoryRepository = inventoryHistoryRepository;
    }

    public InventoryResponse getInventory(Long productId) {
        return InventoryResponse.from(getProduct(productId));
    }

    /**
     * Increase/decrease is expressed as a type + a positive magnitude rather than a signed
     * quantity, and locked the same way order stock changes are (see
     * ProductRepository.findAllByIdInWithPessimisticWrite) — the same row can otherwise be
     * touched concurrently by a customer placing an order.
     */
    @Transactional
    public InventoryAdjustResponse adjust(Long productId, InventoryAdjustRequest request, Long adminMemberId) {
        Product product = productRepository.findAllByIdInWithPessimisticWrite(List.of(productId)).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다."));

        int quantityBefore = product.getStockQuantity();
        int signedChange;

        if (request.type() == InventoryAdjustType.INCREASE) {
            product.increaseStock(request.quantity());
            signedChange = request.quantity();
        } else {
            if (!product.hasEnoughStock(request.quantity())) {
                throw new InvalidRequestException("INVENTORY_ADJUSTMENT_INVALID", "재고보다 많은 수량을 감소시킬 수 없습니다.");
            }
            product.decreaseStock(request.quantity());
            signedChange = -request.quantity();
        }

        InventoryChangeType changeType = signedChange > 0 ? InventoryChangeType.ADMIN_INCREASE : InventoryChangeType.ADMIN_DECREASE;
        inventoryHistoryRepository.save(new InventoryHistory(
                product, changeType, quantityBefore, signedChange, request.reason(), null, null, adminMemberId
        ));

        return InventoryAdjustResponse.of(product, quantityBefore, signedChange);
    }

    public InventoryHistoryPageResponse getHistory(Long productId, int page, int size) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다.");
        }

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        Page<InventoryHistoryResponse> result = inventoryHistoryRepository
                .findByProductIdOrderByCreatedAtDesc(productId, pageable)
                .map(InventoryHistoryResponse::from);

        return InventoryHistoryPageResponse.from(result);
    }

    private Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다."));
    }
}
