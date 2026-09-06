package studio.aroundhub.tunagiftset.repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import studio.aroundhub.tunagiftset.entity.Order;
import studio.aroundhub.tunagiftset.entity.type.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    Optional<Order> findByOrderNumberAndMemberId(String orderNumber, Long memberId);

    Page<Order> findAllByMemberIdOrderByOrderedAtDesc(Long memberId, Pageable pageable);

    @Query("""
            select o
            from Order o
            join fetch o.member
            where o.orderNumber = :orderNumber
              and o.member.id = :memberId
            """)
    Optional<Order> findDetailByOrderNumberAndMemberId(String orderNumber, Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select o
            from Order o
            join fetch o.member
            where o.orderNumber = :orderNumber
              and o.member.id = :memberId
            """)
    Optional<Order> findByOrderNumberAndMemberIdForUpdate(String orderNumber, Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select o
            from Order o
            where o.id = :orderId
            """)
    Optional<Order> findByIdForUpdate(Long orderId);

    // Unscoped by member — for admin operations, which may act on any order.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select o
            from Order o
            where o.orderNumber = :orderNumber
            """)
    Optional<Order> findByOrderNumberForUpdate(String orderNumber);

    @Query("""
            select o
            from Order o
            join fetch o.member
            where o.orderNumber = :orderNumber
            """)
    Optional<Order> findDetailByOrderNumber(String orderNumber);

    long countByOrderStatus(OrderStatus orderStatus);

    long countByOrderedAtBetween(Instant start, Instant end);

    @Query("""
            select o.id
            from Order o
            where o.orderStatus = studio.aroundhub.tunagiftset.entity.type.OrderStatus.PAYMENT_PENDING
              and o.expiresAt is not null
              and o.expiresAt <= :now
            order by o.expiresAt asc
            """)
    List<Long> findExpiredPaymentPendingOrderIds(Instant now, Pageable pageable);

    List<Order> findByIdIn(List<Long> ids);
}
