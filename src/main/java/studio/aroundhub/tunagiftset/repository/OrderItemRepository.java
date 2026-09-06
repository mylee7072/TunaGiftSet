package studio.aroundhub.tunagiftset.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import studio.aroundhub.tunagiftset.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    List<OrderItem> findByOrderIdOrderByIdAsc(Long orderId);

    @Query("""
            select oi
            from OrderItem oi
            join fetch oi.order o
            join fetch o.member
            join fetch oi.product
            where oi.id = :orderItemId
            """)
    Optional<OrderItem> findReviewTargetById(Long orderItemId);

    @Query("""
            select oi
            from OrderItem oi
            join fetch oi.order o
            where o.id in :orderIds
            order by o.id asc, oi.id asc
            """)
    List<OrderItem> findByOrderIdsOrderByOrderIdAndId(List<Long> orderIds);
}
