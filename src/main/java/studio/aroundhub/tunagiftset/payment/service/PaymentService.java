package studio.aroundhub.tunagiftset.payment.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import studio.aroundhub.tunagiftset.coupon.service.CouponService;
import studio.aroundhub.tunagiftset.entity.Delivery;
import studio.aroundhub.tunagiftset.entity.Order;
import studio.aroundhub.tunagiftset.entity.Payment;
import studio.aroundhub.tunagiftset.entity.type.InventoryChangeType;
import studio.aroundhub.tunagiftset.entity.type.OrderStatus;
import studio.aroundhub.tunagiftset.entity.type.PaymentStatus;
import studio.aroundhub.tunagiftset.exception.DuplicateResourceException;
import studio.aroundhub.tunagiftset.exception.InvalidRequestException;
import studio.aroundhub.tunagiftset.exception.ResourceNotFoundException;
import studio.aroundhub.tunagiftset.order.dto.OrderCancelResponse;
import studio.aroundhub.tunagiftset.order.service.StockRestorationService;
import studio.aroundhub.tunagiftset.payment.client.PaymentGatewayClient;
import studio.aroundhub.tunagiftset.payment.client.PaymentProviderException;
import studio.aroundhub.tunagiftset.payment.client.TossPaymentCancelCommand;
import studio.aroundhub.tunagiftset.payment.client.TossPaymentConfirmCommand;
import studio.aroundhub.tunagiftset.payment.client.TossPaymentResult;
import studio.aroundhub.tunagiftset.payment.dto.PaymentConfirmRequest;
import studio.aroundhub.tunagiftset.payment.dto.PaymentConfirmResponse;
import studio.aroundhub.tunagiftset.repository.DeliveryRepository;
import studio.aroundhub.tunagiftset.repository.OrderRepository;
import studio.aroundhub.tunagiftset.repository.PaymentRepository;

/**
 * Confirm/cancel are split into lock-validate / external-call / lock-commit phases so that
 * no database row lock (or connection) is held while waiting on the Toss HTTP call. Each phase
 * re-acquires a pessimistic lock and re-checks state, which is what keeps this safe against the
 * order-expiration scheduler and concurrent duplicate requests racing the same order/payment.
 *
 * <p>Every phase method below uses {@code REQUIRES_NEW} rather than the default {@code REQUIRED}:
 * Boot's Open-in-View binds one EntityManager to the whole HTTP request, so a plain {@code
 * REQUIRED} transaction would reuse whatever copy of Order/Payment an earlier phase already
 * loaded this request instead of re-reading current state — silently defeating the pessimistic
 * re-lock this phase-split design depends on. {@code REQUIRES_NEW} forces a genuinely fresh
 * EntityManager (and therefore a real re-read) each time. As a second line of defense — since
 * pessimistic locks only serialize writers, not readers, so two concurrent requests can both
 * legitimately read "not yet done" before either commits — the commit-phase methods below can
 * still lose an optimistic-version race; {@link #confirm} and {@link #cancelPaidOrder} catch that
 * and resolve it by re-reading the now-current state, since the only way our own update can lose
 * that race is if another concurrent request just committed the same logical transition.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String DEFAULT_CANCEL_REASON = "구매자 요청에 의한 주문 취소";

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final DeliveryRepository deliveryRepository;
    private final StockRestorationService stockRestorationService;
    private final PaymentGatewayClient paymentGatewayClient;
    private final Clock clock;
    private final CouponService couponService;
    private final PaymentService self;

    public PaymentService(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            DeliveryRepository deliveryRepository,
            StockRestorationService stockRestorationService,
            PaymentGatewayClient paymentGatewayClient,
            Clock clock,
            CouponService couponService,
            @Lazy PaymentService self
    ) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.deliveryRepository = deliveryRepository;
        this.stockRestorationService = stockRestorationService;
        this.paymentGatewayClient = paymentGatewayClient;
        this.clock = clock;
        this.couponService = couponService;
        this.self = self;
    }

    public PaymentConfirmResponse confirm(Long memberId, PaymentConfirmRequest request) {
        ConfirmPreparation preparation = self.prepareConfirm(memberId, request);
        if (preparation.alreadyConfirmed()) {
            return preparation.response();
        }

        String idempotencyKey = "confirm:" + preparation.orderNumber() + ":" + request.paymentKey();
        TossPaymentResult result;
        try {
            result = paymentGatewayClient.confirm(new TossPaymentConfirmCommand(
                    request.paymentKey(), request.orderId(), request.amount(), idempotencyKey
            ));
        } catch (PaymentProviderException exception) {
            // The provider may have accepted a request whose response was lost.
            // Keep IN_PROGRESS so only the same payment key can safely retry it.
            if (!exception.hasUnknownOutcome()) {
                self.markConfirmFailed(preparation.paymentId(), exception);
            }
            throw exception;
        }

        try {
            return self.completeConfirm(preparation.orderId(), result);
        } catch (ObjectOptimisticLockingFailureException exception) {
            return self.currentConfirmState(preparation.orderId());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentConfirmResponse currentConfirmState(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "Order was not found."));
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("PAYMENT_NOT_FOUND", "Payment was not found."));
        return PaymentConfirmResponse.from(order, payment);
    }

    // See the class-level note on REQUIRES_NEW.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ConfirmPreparation prepareConfirm(Long memberId, PaymentConfirmRequest request) {
        Order order = orderRepository.findByOrderNumberAndMemberIdForUpdate(request.orderId(), memberId)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "Order was not found."));
        Payment payment = paymentRepository.findByOrderIdForUpdate(order.getId())
                .orElseThrow(() -> new ResourceNotFoundException("PAYMENT_NOT_FOUND", "Payment was not found."));

        if (payment.getStatus() == PaymentStatus.PAID) {
            if (Objects.equals(payment.getPaymentKey(), request.paymentKey())) {
                return ConfirmPreparation.alreadyConfirmed(PaymentConfirmResponse.from(order, payment));
            }
            throw new DuplicateResourceException("PAYMENT_ALREADY_COMPLETED", "Order has already been paid.");
        }
        if (payment.getStatus() == PaymentStatus.CANCELED) {
            throw new InvalidRequestException("PAYMENT_CANCELED", "Payment for this order has been canceled.");
        }
        if (payment.getStatus() == PaymentStatus.IN_PROGRESS
                && !Objects.equals(payment.getPaymentKey(), request.paymentKey())) {
            throw new DuplicateResourceException(
                    "PAYMENT_CONFIRM_IN_PROGRESS",
                    "Payment confirmation is already in progress. Retry with the same payment key."
            );
        }
        if (order.getOrderStatus() == OrderStatus.EXPIRED) {
            throw new InvalidRequestException("ORDER_EXPIRED", "Order has expired.");
        }
        if (order.getOrderStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new InvalidRequestException("PAYMENT_NOT_ALLOWED", "Order is not awaiting payment.");
        }
        if (order.getExpiresAt() != null && !order.getExpiresAt().isAfter(clock.instant())) {
            throw new InvalidRequestException("ORDER_EXPIRED", "Order has expired.");
        }
        if (order.getTotalAmount().compareTo(request.amount()) != 0) {
            log.warn("Payment amount mismatch. orderNumber={} expectedAmount={} requestedAmount={}",
                    order.getOrderNumber(), order.getTotalAmount(), request.amount());
            throw new InvalidRequestException("PAYMENT_AMOUNT_MISMATCH", "Payment amount does not match the order amount.");
        }
        if (paymentRepository.existsByPaymentKeyAndOrderIdNot(request.paymentKey(), order.getId())) {
            throw new DuplicateResourceException("PAYMENT_DUPLICATE_REQUEST", "Payment key is already used by another order.");
        }

        payment.startConfirm(request.paymentKey());
        return ConfirmPreparation.of(order.getId(), payment.getId(), order.getOrderNumber());
    }

    // See the class-level note on REQUIRES_NEW.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markConfirmFailed(Long paymentId, PaymentProviderException exception) {
        paymentRepository.findById(paymentId)
                .ifPresent(payment -> payment.markFailed(exception.getProviderCode(), "Toss payment confirmation failed."));
    }

    // See the class-level note on REQUIRES_NEW.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentConfirmResponse completeConfirm(Long orderId, TossPaymentResult result) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "Order was not found."));
        Payment payment = paymentRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("PAYMENT_NOT_FOUND", "Payment was not found."));

        if (payment.getStatus() == PaymentStatus.PAID) {
            return PaymentConfirmResponse.from(order, payment);
        }
        if (order.getTotalAmount().compareTo(result.totalAmount()) != 0) {
            payment.markFailed("AMOUNT_MISMATCH_AFTER_CONFIRM", "Approved amount did not match the order amount.");
            log.error("CRITICAL: Toss approved amount differs from order amount. orderNumber={} orderAmount={} approvedAmount={}",
                    order.getOrderNumber(), order.getTotalAmount(), result.totalAmount());
            throw new InvalidRequestException("PAYMENT_AMOUNT_MISMATCH", "Approved amount did not match the order amount.");
        }
        if (order.getOrderStatus() != OrderStatus.PAYMENT_PENDING) {
            payment.markPaid(result.paymentKey(), result.method(), result.totalAmount(), result.currency(), resolveApprovedAt(result));
            log.error("CRITICAL: Toss confirmed payment but order is no longer payable; manual reconciliation required. "
                            + "orderNumber={} orderStatus={} paymentKey={}",
                    order.getOrderNumber(), order.getOrderStatus(), mask(result.paymentKey()));
            throw new InvalidRequestException("PAYMENT_ORDER_STATE_CONFLICT",
                    "결제는 처리되었으나 주문 상태 확인이 필요합니다. 고객센터에 문의해 주세요.");
        }

        payment.markPaid(result.paymentKey(), result.method(), result.totalAmount(), result.currency(), resolveApprovedAt(result));
        order.markPaid();
        couponService.markUsedForOrder(order);
        log.info("Payment confirmed. orderNumber={} paymentKey={} amount={}",
                order.getOrderNumber(), mask(result.paymentKey()), result.totalAmount());

        return PaymentConfirmResponse.from(order, payment);
    }

    public OrderCancelResponse cancelPaidOrder(Long memberId, String orderNumber) {
        return runCancel(self.prepareCancel(memberId, orderNumber));
    }

    /** Admin equivalent — looks up the order by number alone (no ownership check) and stamps the history with adminMemberId. */
    public OrderCancelResponse adminCancelPaidOrder(String orderNumber, Long adminMemberId, String reason) {
        return runCancel(self.prepareAdminCancel(orderNumber, adminMemberId, reason));
    }

    private OrderCancelResponse runCancel(CancelPreparation preparation) {
        if (preparation.alreadyCanceled()) {
            return preparation.response();
        }

        String idempotencyKey = "cancel:" + preparation.paymentKey();
        TossPaymentResult result;
        try {
            result = paymentGatewayClient.cancel(new TossPaymentCancelCommand(
                    preparation.paymentKey(), preparation.cancelReason(), null, idempotencyKey
            ));
        } catch (PaymentProviderException exception) {
            log.warn("Toss cancel failed. orderId={} providerCode={}", preparation.orderId(), exception.getProviderCode());
            throw exception;
        }

        try {
            return self.completeCancel(preparation.orderId(), result, preparation.adminMemberId());
        } catch (ObjectOptimisticLockingFailureException exception) {
            return self.currentCancelState(preparation.orderId());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OrderCancelResponse currentCancelState(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "Order was not found."));
        return OrderCancelResponse.from(order);
    }

    // See the class-level note on REQUIRES_NEW.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CancelPreparation prepareCancel(Long memberId, String orderNumber) {
        Order order = orderRepository.findByOrderNumberAndMemberIdForUpdate(orderNumber, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "Order was not found."));
        return prepareCancelLocked(order, null, DEFAULT_CANCEL_REASON);
    }

    // See the class-level note on REQUIRES_NEW.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CancelPreparation prepareAdminCancel(String orderNumber, Long adminMemberId, String reason) {
        Order order = orderRepository.findByOrderNumberForUpdate(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "Order was not found."));
        String tossReason = reason == null || reason.isBlank() ? "관리자에 의한 주문 취소" : reason;
        return prepareCancelLocked(order, adminMemberId, tossReason);
    }

    /**
     * Shared validation for an already row-locked order. Eligibility is PAID payment + not yet
     * shipped — deliberately not "order status == PAID", so a PREPARING order (see
     * {@link studio.aroundhub.tunagiftset.order.service.OrderService#startPreparing}) whose payment
     * is still PAID can also be routed through Toss cancellation.
     */
    private CancelPreparation prepareCancelLocked(Order order, Long adminMemberId, String cancelReason) {
        Payment payment = paymentRepository.findByOrderIdForUpdate(order.getId())
                .orElseThrow(() -> new ResourceNotFoundException("PAYMENT_NOT_FOUND", "Payment was not found."));

        if (order.getOrderStatus() == OrderStatus.CANCELED || payment.getStatus() == PaymentStatus.CANCELED) {
            return CancelPreparation.alreadyCanceled(OrderCancelResponse.from(order));
        }
        if (order.getOrderStatus() == OrderStatus.SHIPPING || order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new InvalidRequestException("ORDER_CANNOT_BE_CANCELED", "Order cannot be canceled in the current status.");
        }
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new InvalidRequestException("ORDER_CANNOT_BE_CANCELED", "Order cannot be canceled in the current status.");
        }

        return CancelPreparation.of(order.getId(), payment.getPaymentKey(), cancelReason, adminMemberId);
    }

    // See the class-level note on REQUIRES_NEW.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OrderCancelResponse completeCancel(Long orderId, TossPaymentResult result, Long adminMemberId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "Order was not found."));
        Payment payment = paymentRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("PAYMENT_NOT_FOUND", "Payment was not found."));

        if (payment.getStatus() == PaymentStatus.CANCELED) {
            return OrderCancelResponse.from(order);
        }

        payment.markCanceled(resolveCanceledAt(result));
        order.cancel();
        deliveryRepository.findByOrderId(orderId).ifPresent(Delivery::cancel);
        String reason = adminMemberId == null ? "고객 요청에 의한 결제 취소" : "관리자에 의한 결제 취소";
        stockRestorationService.restore(orderId, InventoryChangeType.ORDER_CANCEL, reason, adminMemberId);
        couponService.restoreForOrder(order);

        log.info("Payment canceled. orderNumber={} paymentKey={}", order.getOrderNumber(), mask(payment.getPaymentKey()));

        return OrderCancelResponse.from(order);
    }

    private Instant resolveApprovedAt(TossPaymentResult result) {
        return result.approvedAt() != null ? result.approvedAt() : clock.instant();
    }

    private Instant resolveCanceledAt(TossPaymentResult result) {
        return result.canceledAt() != null ? result.canceledAt() : clock.instant();
    }

    private String mask(String value) {
        if (value == null || value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    private record ConfirmPreparation(
            Long orderId,
            Long paymentId,
            String orderNumber,
            boolean alreadyConfirmed,
            PaymentConfirmResponse response
    ) {
        static ConfirmPreparation of(Long orderId, Long paymentId, String orderNumber) {
            return new ConfirmPreparation(orderId, paymentId, orderNumber, false, null);
        }

        static ConfirmPreparation alreadyConfirmed(PaymentConfirmResponse response) {
            return new ConfirmPreparation(null, null, null, true, response);
        }
    }

    private record CancelPreparation(
            Long orderId,
            String paymentKey,
            String cancelReason,
            Long adminMemberId,
            boolean alreadyCanceled,
            OrderCancelResponse response
    ) {
        static CancelPreparation of(Long orderId, String paymentKey, String cancelReason, Long adminMemberId) {
            return new CancelPreparation(orderId, paymentKey, cancelReason, adminMemberId, false, null);
        }

        static CancelPreparation alreadyCanceled(OrderCancelResponse response) {
            return new CancelPreparation(null, null, null, null, true, response);
        }
    }
}
