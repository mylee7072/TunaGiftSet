package studio.aroundhub.tunagiftset.payment.scheduler;

import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import studio.aroundhub.tunagiftset.coupon.service.CouponService;
import studio.aroundhub.tunagiftset.entity.Order;
import studio.aroundhub.tunagiftset.entity.Payment;
import studio.aroundhub.tunagiftset.entity.type.InventoryChangeType;
import studio.aroundhub.tunagiftset.entity.type.OrderStatus;
import studio.aroundhub.tunagiftset.entity.type.PaymentStatus;
import studio.aroundhub.tunagiftset.order.service.StockRestorationService;
import studio.aroundhub.tunagiftset.repository.OrderRepository;
import studio.aroundhub.tunagiftset.repository.PaymentRepository;

/**
 * Expires PAYMENT_PENDING orders whose expiresAt has passed and returns their stock. Each
 * candidate order is expired in its own short transaction (via the self-proxy) so one locked or
 * failing order never blocks the rest of the batch, and so no lock is held for the whole sweep.
 *
 * <p>Race safety: a payment actively being confirmed (status IN_PROGRESS) is never expired here,
 * even if expiresAt has already passed — {@link studio.aroundhub.tunagiftset.payment.service.PaymentService}
 * re-locks and re-checks the order status before marking it PAID, so the two paths cannot both
 * "win". This also makes the sweep safe to run from multiple instances: whichever instance wins
 * the row lock for an order proceeds, the other reloads and finds nothing left to do.
 */
@Component
@ConditionalOnProperty(prefix = "payment.expiration.scheduler", name = "enabled", havingValue = "true")
public class OrderExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderExpirationScheduler.class);
    private static final int BATCH_SIZE = 100;

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final StockRestorationService stockRestorationService;
    private final CouponService couponService;
    private final Clock clock;
    private final OrderExpirationScheduler self;

    public OrderExpirationScheduler(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            StockRestorationService stockRestorationService,
            CouponService couponService,
            Clock clock,
            @Lazy OrderExpirationScheduler self
    ) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.stockRestorationService = stockRestorationService;
        this.couponService = couponService;
        this.clock = clock;
        this.self = self;
    }

    @Scheduled(fixedDelayString = "${payment.expiration.scheduler.interval-millis:60000}")
    public void expirePendingOrders() {
        List<Long> candidateOrderIds = orderRepository.findExpiredPaymentPendingOrderIds(clock.instant(), PageRequest.of(0, BATCH_SIZE));
        if (candidateOrderIds.isEmpty()) {
            return;
        }

        int expiredCount = 0;
        for (Long orderId : candidateOrderIds) {
            try {
                if (self.expireOne(orderId)) {
                    expiredCount++;
                }
            } catch (RuntimeException exception) {
                log.error("Failed to expire order. orderId={}", orderId, exception);
            }
        }
        if (expiredCount > 0) {
            log.info("Expired {} payment-pending order(s).", expiredCount);
        }
    }

    @Transactional
    public boolean expireOne(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElse(null);
        if (order == null || order.getOrderStatus() != OrderStatus.PAYMENT_PENDING) {
            return false;
        }
        if (order.getExpiresAt() == null || order.getExpiresAt().isAfter(clock.instant())) {
            return false;
        }

        Payment payment = paymentRepository.findByOrderIdForUpdate(orderId).orElse(null);
        if (payment != null && (payment.getStatus() == PaymentStatus.IN_PROGRESS || payment.getStatus() == PaymentStatus.PAID)) {
            log.warn("Skipping expiration; a confirmation is in flight. orderNumber={} paymentStatus={}",
                    order.getOrderNumber(), payment.getStatus());
            return false;
        }

        order.expire();
        if (payment != null) {
            payment.markFailed("ORDER_EXPIRED", "Payment deadline expired.");
        }
        stockRestorationService.restore(orderId, InventoryChangeType.ORDER_CANCEL, "결제 시간 초과로 주문 만료", null);
        couponService.restoreForOrder(order);
        log.info("Order expired. orderNumber={}", order.getOrderNumber());
        return true;
    }
}
