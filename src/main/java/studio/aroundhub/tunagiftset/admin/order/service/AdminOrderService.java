package studio.aroundhub.tunagiftset.admin.order.service;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studio.aroundhub.tunagiftset.admin.order.dto.AdminDeliveryUpdateRequest;
import studio.aroundhub.tunagiftset.admin.order.dto.AdminOrderDetailResponse;
import studio.aroundhub.tunagiftset.admin.order.dto.AdminOrderListResponse;
import studio.aroundhub.tunagiftset.admin.order.dto.AdminOrderPageResponse;
import studio.aroundhub.tunagiftset.entity.Delivery;
import studio.aroundhub.tunagiftset.entity.Order;
import studio.aroundhub.tunagiftset.entity.OrderItem;
import studio.aroundhub.tunagiftset.entity.Payment;
import studio.aroundhub.tunagiftset.entity.type.DeliveryStatus;
import studio.aroundhub.tunagiftset.entity.type.OrderStatus;
import studio.aroundhub.tunagiftset.entity.type.PaymentStatus;
import studio.aroundhub.tunagiftset.exception.InvalidRequestException;
import studio.aroundhub.tunagiftset.exception.ResourceNotFoundException;
import studio.aroundhub.tunagiftset.order.dto.OrderCancelResponse;
import studio.aroundhub.tunagiftset.order.dto.OrderItemResponse;
import studio.aroundhub.tunagiftset.order.service.OrderService;
import studio.aroundhub.tunagiftset.repository.DeliveryRepository;
import studio.aroundhub.tunagiftset.repository.OrderItemRepository;
import studio.aroundhub.tunagiftset.repository.OrderRepository;
import studio.aroundhub.tunagiftset.repository.PaymentRepository;

/**
 * Orchestrates admin-facing order operations. Cancellation reuses
 * {@link OrderService#adminCancel} (and, transitively, PaymentService's Toss-cancel flow) rather
 * than re-implementing the cancel state machine here — this class owns only what has no
 * customer-facing equivalent: search, detail, and the PAID→PREPARING→SHIPPING→DELIVERED walk.
 */
@Service
@Transactional(readOnly = true)
public class AdminOrderService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final DeliveryRepository deliveryRepository;
    private final OrderService orderService;
    private final Clock clock;

    public AdminOrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            PaymentRepository paymentRepository,
            DeliveryRepository deliveryRepository,
            OrderService orderService,
            Clock clock
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.deliveryRepository = deliveryRepository;
        this.orderService = orderService;
        this.clock = clock;
    }

    public AdminOrderPageResponse findOrders(
            String orderNumber,
            String memberEmail,
            String recipientName,
            String recipientPhone,
            OrderStatus orderStatus,
            PaymentStatus paymentStatus,
            DeliveryStatus deliveryStatus,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "orderedAt")
        );

        Specification<Order> specification = orderNumberContains(orderNumber)
                .and(memberEmailContains(memberEmail))
                .and(recipientNameContains(recipientName))
                .and(recipientPhoneContains(recipientPhone))
                .and(orderStatusEquals(orderStatus))
                .and(paymentStatusEquals(paymentStatus))
                .and(deliveryStatusEquals(deliveryStatus))
                .and(orderedAtBetween(toStartInstant(startDate), toEndInstant(endDate)));

        Page<Order> orders = orderRepository.findAll(specification, pageable);
        List<Long> orderIds = orders.getContent().stream().map(Order::getId).toList();

        Map<Long, PaymentStatus> paymentStatusByOrderId = paymentRepository.findByOrderIdIn(orderIds).stream()
                .collect(Collectors.toMap(payment -> payment.getOrder().getId(), Payment::getStatus));
        Map<Long, DeliveryStatus> deliveryStatusByOrderId = deliveryRepository.findByOrderIdIn(orderIds).stream()
                .collect(Collectors.toMap(delivery -> delivery.getOrder().getId(), Delivery::getStatus));
        Map<Long, List<OrderItem>> itemsByOrderId = itemsByOrderId(orderIds);

        return AdminOrderPageResponse.from(orders.map(order -> new AdminOrderListResponse(
                order.getOrderNumber(),
                order.getMember().getEmail(),
                order.getOrderStatus(),
                paymentStatusByOrderId.get(order.getId()),
                deliveryStatusByOrderId.get(order.getId()),
                representativeProductName(itemsByOrderId.getOrDefault(order.getId(), List.of())),
                itemsByOrderId.getOrDefault(order.getId(), List.of()).stream().mapToInt(OrderItem::getQuantity).sum(),
                order.getTotalAmount(),
                order.getOrderedAt()
        )));
    }

    public AdminOrderDetailResponse findOrder(String orderNumber) {
        Order order = orderRepository.findDetailByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "Order was not found."));
        List<OrderItemResponse> items = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId()).stream()
                .map(OrderItemResponse::from)
                .toList();
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        Delivery delivery = deliveryRepository.findByOrderId(order.getId()).orElse(null);

        return AdminOrderDetailResponse.from(order, items, payment, delivery);
    }

    @Transactional
    public AdminOrderDetailResponse prepare(String orderNumber) {
        Order order = lockOrder(orderNumber);
        if (order.getOrderStatus() != OrderStatus.PAID) {
            throw new InvalidRequestException("ORDER_CANNOT_BE_PREPARED", "Only a paid order can start preparing.");
        }
        order.startPreparing();
        deliveryRepository.findByOrderId(order.getId()).ifPresent(Delivery::startPreparing);

        return findOrder(orderNumber);
    }

    @Transactional
    public AdminOrderDetailResponse registerDelivery(String orderNumber, AdminDeliveryUpdateRequest request) {
        Order order = lockOrder(orderNumber);
        if (order.getOrderStatus() != OrderStatus.PREPARING) {
            throw new InvalidRequestException("INVALID_ORDER_STATUS", "Tracking information can only be registered while preparing.");
        }
        Delivery delivery = deliveryRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new ResourceNotFoundException("DELIVERY_NOT_FOUND", "Delivery was not found."));
        delivery.registerTracking(request.carrier(), request.trackingNumber());

        return findOrder(orderNumber);
    }

    @Transactional
    public AdminOrderDetailResponse ship(String orderNumber) {
        Order order = lockOrder(orderNumber);
        if (order.getOrderStatus() != OrderStatus.PREPARING) {
            throw new InvalidRequestException("ORDER_CANNOT_BE_SHIPPED", "Only a preparing order can start shipping.");
        }
        Delivery delivery = deliveryRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new ResourceNotFoundException("DELIVERY_NOT_FOUND", "Delivery was not found."));
        if (delivery.getTrackingNumber() == null || delivery.getTrackingNumber().isBlank()) {
            throw new InvalidRequestException("TRACKING_NUMBER_REQUIRED", "Tracking number must be registered before shipping.");
        }

        Instant now = clock.instant();
        delivery.ship(now);
        order.ship();

        return findOrder(orderNumber);
    }

    @Transactional
    public AdminOrderDetailResponse deliver(String orderNumber) {
        Order order = lockOrder(orderNumber);
        if (order.getOrderStatus() != OrderStatus.SHIPPING) {
            throw new InvalidRequestException("ORDER_CANNOT_BE_DELIVERED", "Only a shipping order can be marked delivered.");
        }
        Delivery delivery = deliveryRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new ResourceNotFoundException("DELIVERY_NOT_FOUND", "Delivery was not found."));

        Instant now = clock.instant();
        delivery.deliver(now);
        order.deliver();

        return findOrder(orderNumber);
    }

    public OrderCancelResponse cancel(String orderNumber, Long adminMemberId, String reason) {
        return orderService.adminCancel(orderNumber, adminMemberId, reason);
    }

    private Order lockOrder(String orderNumber) {
        return orderRepository.findByOrderNumberForUpdate(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "Order was not found."));
    }

    private Map<Long, List<OrderItem>> itemsByOrderId(List<Long> orderIds) {
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<OrderItem>> result = new HashMap<>();
        for (OrderItem orderItem : orderItemRepository.findByOrderIdsOrderByOrderIdAndId(orderIds)) {
            result.computeIfAbsent(orderItem.getOrder().getId(), ignored -> new ArrayList<>()).add(orderItem);
        }
        return result;
    }

    private String representativeProductName(List<OrderItem> items) {
        if (items.isEmpty()) {
            return "";
        }
        if (items.size() == 1) {
            return items.get(0).getProductName();
        }
        return items.get(0).getProductName() + " 외 " + (items.size() - 1) + "건";
    }

    // Admin search dates are calendar days in KST, matching how an operator reading a Korean
    // storefront's dashboard thinks about "today" — regardless of the server's own OS time zone.
    private Instant toStartInstant(LocalDate date) {
        return date == null ? null : date.atStartOfDay(SEOUL_ZONE).toInstant();
    }

    private Instant toEndInstant(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay(SEOUL_ZONE).toInstant();
    }

    private Specification<Order> orderNumberContains(String value) {
        return (root, query, cb) -> value == null || value.isBlank()
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("orderNumber")), "%" + value.trim().toLowerCase() + "%");
    }

    private Specification<Order> memberEmailContains(String value) {
        return (root, query, cb) -> value == null || value.isBlank()
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("member").get("email")), "%" + value.trim().toLowerCase() + "%");
    }

    private Specification<Order> recipientNameContains(String value) {
        return (root, query, cb) -> value == null || value.isBlank()
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("recipientName")), "%" + value.trim().toLowerCase() + "%");
    }

    private Specification<Order> recipientPhoneContains(String value) {
        return (root, query, cb) -> value == null || value.isBlank()
                ? cb.conjunction()
                : cb.like(root.get("recipientPhone"), "%" + value.trim() + "%");
    }

    private Specification<Order> orderStatusEquals(OrderStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("orderStatus"), status);
    }

    private Specification<Order> paymentStatusEquals(PaymentStatus status) {
        return (root, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Payment> paymentRoot = subquery.from(Payment.class);
            subquery.select(paymentRoot.get("order").get("id")).where(cb.equal(paymentRoot.get("status"), status));
            return root.get("id").in(subquery);
        };
    }

    private Specification<Order> deliveryStatusEquals(DeliveryStatus status) {
        return (root, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Delivery> deliveryRoot = subquery.from(Delivery.class);
            subquery.select(deliveryRoot.get("order").get("id")).where(cb.equal(deliveryRoot.get("status"), status));
            return root.get("id").in(subquery);
        };
    }

    private Specification<Order> orderedAtBetween(Instant start, Instant end) {
        return (root, query, cb) -> {
            if (start == null && end == null) {
                return cb.conjunction();
            }
            if (start != null && end != null) {
                return cb.and(cb.greaterThanOrEqualTo(root.get("orderedAt"), start), cb.lessThan(root.get("orderedAt"), end));
            }
            if (start != null) {
                return cb.greaterThanOrEqualTo(root.get("orderedAt"), start);
            }
            return cb.lessThan(root.get("orderedAt"), end);
        };
    }
}
