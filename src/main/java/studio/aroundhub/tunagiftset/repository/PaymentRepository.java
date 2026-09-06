package studio.aroundhub.tunagiftset.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import studio.aroundhub.tunagiftset.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from Payment p
            where p.order.id = :orderId
            """)
    Optional<Payment> findByOrderIdForUpdate(Long orderId);

    Optional<Payment> findByPaymentKey(String paymentKey);

    boolean existsByPaymentKey(String paymentKey);

    boolean existsByPaymentKeyAndOrderIdNot(String paymentKey, Long orderId);

    List<Payment> findByOrderIdIn(List<Long> orderIds);

    // Revenue is defined as approved (PAID) payments only — canceled/refunded and
    // still-pending orders never count (see AdminDashboardService).
    @Query("""
            select coalesce(sum(p.approvedAmount), 0)
            from Payment p
            where p.status = studio.aroundhub.tunagiftset.entity.type.PaymentStatus.PAID
              and p.paidAt >= :start and p.paidAt < :end
            """)
    BigDecimal sumApprovedAmountByPaidAtBetween(Instant start, Instant end);
}
