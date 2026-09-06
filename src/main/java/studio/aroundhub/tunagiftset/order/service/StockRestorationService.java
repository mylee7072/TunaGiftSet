package studio.aroundhub.tunagiftset.order.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import studio.aroundhub.tunagiftset.entity.InventoryHistory;
import studio.aroundhub.tunagiftset.entity.OrderItem;
import studio.aroundhub.tunagiftset.entity.Product;
import studio.aroundhub.tunagiftset.entity.type.InventoryChangeType;
import studio.aroundhub.tunagiftset.repository.InventoryHistoryRepository;
import studio.aroundhub.tunagiftset.repository.OrderItemRepository;
import studio.aroundhub.tunagiftset.repository.ProductRepository;

@Component
public class StockRestorationService {

    private static final String REFERENCE_TYPE_ORDER = "ORDER";

    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;

    public StockRestorationService(
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository,
            InventoryHistoryRepository inventoryHistoryRepository
    ) {
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.inventoryHistoryRepository = inventoryHistoryRepository;
    }

    /**
     * Gives back the stock an order's items had reserved. {@code adminMemberId} is
     * non-null only when an admin action (not the customer or the expiration
     * scheduler) triggered the restoration, so the history ledger can tell them apart.
     */
    public void restore(Long orderId, InventoryChangeType changeType, String reason, Long adminMemberId) {
        List<OrderItem> orderItems = orderItemRepository.findByOrderIdOrderByIdAsc(orderId);
        List<Long> productIds = orderItems.stream()
                .map(orderItem -> orderItem.getProduct().getId())
                .distinct()
                .sorted()
                .toList();
        Map<Long, Product> lockedProducts = productRepository.findAllByIdInWithPessimisticWrite(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        for (OrderItem orderItem : orderItems) {
            Product product = lockedProducts.get(orderItem.getProduct().getId());
            if (product == null) {
                continue;
            }
            int quantityBefore = product.getStockQuantity();
            product.increaseStock(orderItem.getQuantity());
            inventoryHistoryRepository.save(new InventoryHistory(
                    product, changeType, quantityBefore, orderItem.getQuantity(), reason, REFERENCE_TYPE_ORDER, orderId, adminMemberId
            ));
        }
    }
}
