package studio.aroundhub.tunagiftset.order.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import studio.aroundhub.tunagiftset.cart.service.ShippingFeePolicy;
import studio.aroundhub.tunagiftset.coupon.service.CouponService;
import studio.aroundhub.tunagiftset.coupon.service.DiscountCalculation;
import studio.aroundhub.tunagiftset.entity.Address;
import studio.aroundhub.tunagiftset.entity.Cart;
import studio.aroundhub.tunagiftset.entity.CartItem;
import studio.aroundhub.tunagiftset.entity.Delivery;
import studio.aroundhub.tunagiftset.entity.InventoryHistory;
import studio.aroundhub.tunagiftset.entity.Member;
import studio.aroundhub.tunagiftset.entity.Order;
import studio.aroundhub.tunagiftset.entity.OrderItem;
import studio.aroundhub.tunagiftset.entity.Payment;
import studio.aroundhub.tunagiftset.entity.Product;
import studio.aroundhub.tunagiftset.entity.type.DeliveryStatus;
import studio.aroundhub.tunagiftset.entity.type.InventoryChangeType;
import studio.aroundhub.tunagiftset.entity.type.OrderStatus;
import studio.aroundhub.tunagiftset.entity.type.PaymentStatus;
import studio.aroundhub.tunagiftset.entity.type.ProductStatus;
import studio.aroundhub.tunagiftset.exception.InvalidRequestException;
import studio.aroundhub.tunagiftset.exception.ResourceNotFoundException;
import studio.aroundhub.tunagiftset.member.service.AddressService;
import studio.aroundhub.tunagiftset.order.dto.OrderCancelResponse;
import studio.aroundhub.tunagiftset.order.dto.OrderCreateRequest;
import studio.aroundhub.tunagiftset.order.dto.OrderDeliveryResponse;
import studio.aroundhub.tunagiftset.order.dto.OrderDetailResponse;
import studio.aroundhub.tunagiftset.order.dto.OrderItemResponse;
import studio.aroundhub.tunagiftset.order.dto.OrderListResponse;
import studio.aroundhub.tunagiftset.order.dto.OrderPageResponse;
import studio.aroundhub.tunagiftset.order.dto.OrderPaymentResponse;
import studio.aroundhub.tunagiftset.payment.service.PaymentService;
import studio.aroundhub.tunagiftset.repository.CartItemRepository;
import studio.aroundhub.tunagiftset.repository.CartRepository;
import studio.aroundhub.tunagiftset.repository.DeliveryRepository;
import studio.aroundhub.tunagiftset.repository.InventoryHistoryRepository;
import studio.aroundhub.tunagiftset.repository.MemberRepository;
import studio.aroundhub.tunagiftset.repository.OrderItemRepository;
import studio.aroundhub.tunagiftset.repository.OrderRepository;
import studio.aroundhub.tunagiftset.repository.PaymentRepository;
import studio.aroundhub.tunagiftset.repository.ProductRepository;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private static final int MAX_PAGE_SIZE = 100;

    private final MemberRepository memberRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final DeliveryRepository deliveryRepository;
    private final ShippingFeePolicy shippingFeePolicy;
    private final OrderNumberGenerator orderNumberGenerator;
    private final StockRestorationService stockRestorationService;
    private final PaymentService paymentService;
    private final InventoryHistoryRepository inventoryHistoryRepository;
    private final AddressService addressService;
    private final CouponService couponService;
    private final OrderService self;

    public OrderService(
            MemberRepository memberRepository,
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            PaymentRepository paymentRepository,
            DeliveryRepository deliveryRepository,
            ShippingFeePolicy shippingFeePolicy,
            OrderNumberGenerator orderNumberGenerator,
            StockRestorationService stockRestorationService,
            PaymentService paymentService,
            InventoryHistoryRepository inventoryHistoryRepository,
            AddressService addressService,
            CouponService couponService,
            @Lazy OrderService self
    ) {
        this.memberRepository = memberRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.deliveryRepository = deliveryRepository;
        this.shippingFeePolicy = shippingFeePolicy;
        this.orderNumberGenerator = orderNumberGenerator;
        this.stockRestorationService = stockRestorationService;
        this.paymentService = paymentService;
        this.inventoryHistoryRepository = inventoryHistoryRepository;
        this.addressService = addressService;
        this.couponService = couponService;
        this.self = self;
    }

    @Transactional
    public OrderDetailResponse createOrder(Long memberId, OrderCreateRequest request) {
        validateCartItemIds(request.cartItemIds());
        Member member = getMember(memberId);
        ShippingAddress shippingAddress = resolveShippingAddress(memberId, request);
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("CART_ITEM_NOT_FOUND", "Orderable cart items were not found."));

        List<CartItem> cartItems = cartItemRepository.findByIdInAndCartId(request.cartItemIds(), cart.getId());
        if (cartItems.size() != request.cartItemIds().size()) {
            throw new ResourceNotFoundException("CART_ITEM_NOT_FOUND", "Orderable cart items were not found.");
        }

        List<Long> productIds = cartItems.stream()
                .map(cartItem -> cartItem.getProduct().getId())
                .distinct()
                .sorted()
                .toList();
        Map<Long, Product> lockedProducts = productRepository.findAllByIdInWithPessimisticWrite(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        BigDecimal productAmount = BigDecimal.ZERO;
        List<OrderItemDraft> drafts = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            Product product = lockedProducts.get(cartItem.getProduct().getId());
            if (product == null) {
                throw new ResourceNotFoundException("PRODUCT_NOT_FOUND", "Product was not found.");
            }
            validateProductForOrder(product, cartItem.getQuantity());
            BigDecimal unitPrice = product.getSalePrice();
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            productAmount = productAmount.add(totalPrice);
            drafts.add(new OrderItemDraft(product, cartItem.getQuantity(), unitPrice));
        }

        DiscountCalculation discount = couponService.calculateForProductAmount(memberId, request.memberCouponId(), productAmount, true);
        Instant orderedAt = Instant.now();
        Order order = orderRepository.save(new Order(
                generateUniqueOrderNumber(),
                member,
                OrderStatus.PAYMENT_PENDING,
                shippingAddress.recipientName(),
                shippingAddress.recipientPhone(),
                shippingAddress.zipCode(),
                shippingAddress.roadAddress(),
                shippingAddress.detailAddress(),
                shippingAddress.jibunAddress(),
                shippingAddress.extraAddress(),
                shippingAddress.deliveryMessage(),
                productAmount,
                discount.shippingFee(),
                discount.promotionDiscountAmount(),
                discount.couponDiscountAmount(),
                discount.memberCoupon() == null ? null : discount.memberCoupon().getCoupon().getName(),
                discount.memberCoupon() == null ? null : discount.memberCoupon().getCoupon().getCode(),
                discount.memberCoupon() == null ? null : discount.memberCoupon().getCoupon().getDiscountType(),
                discount.memberCoupon() == null ? null : discount.memberCoupon().getCoupon().getDiscountValue(),
                discount.totalAmount(),
                orderedAt,
                orderedAt.plusSeconds(30 * 60)
        ));
        couponService.markReserved(discount.memberCoupon(), order);

        List<OrderItem> orderItems = drafts.stream()
                .map(draft -> {
                    int quantityBefore = draft.product().getStockQuantity();
                    draft.product().decreaseStock(draft.quantity());
                    inventoryHistoryRepository.save(new InventoryHistory(
                            draft.product(), InventoryChangeType.ORDER, quantityBefore, -draft.quantity(),
                            "주문 생성", "ORDER", order.getId(), null
                    ));
                    return new OrderItem(
                            order,
                            draft.product(),
                            draft.product().getName(),
                            draft.product().getProductCode(),
                            draft.unitPrice(),
                            draft.quantity()
                    );
                })
                .toList();
        orderItemRepository.saveAll(orderItems);
        Payment payment = paymentRepository.save(new Payment(order, discount.totalAmount(), PaymentStatus.READY));
        Delivery delivery = deliveryRepository.save(new Delivery(order, DeliveryStatus.READY));
        cartItemRepository.deleteAll(cartItems);

        return OrderDetailResponse.from(
                order,
                orderItems.stream().map(OrderItemResponse::from).toList(),
                OrderPaymentResponse.from(payment),
                OrderDeliveryResponse.from(delivery)
        );
    }

    public OrderPageResponse findOrders(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        Page<Order> orders = orderRepository.findAllByMemberIdOrderByOrderedAtDesc(memberId, pageable);
        List<Long> orderIds = orders.getContent().stream().map(Order::getId).toList();
        Map<Long, List<OrderItem>> orderItemsByOrderId = orderItemsByOrderId(orderIds);

        return OrderPageResponse.from(orders.map(order ->
                OrderListResponse.from(order, orderItemsByOrderId.getOrDefault(order.getId(), List.of()))));
    }

    public OrderDetailResponse findOrder(Long memberId, String orderNumber) {
        Order order = getOwnedOrder(memberId, orderNumber);
        List<OrderItemResponse> items = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId()).stream()
                .map(OrderItemResponse::from)
                .toList();
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        Delivery delivery = deliveryRepository.findByOrderId(order.getId()).orElse(null);

        return OrderDetailResponse.from(
                order,
                items,
                OrderPaymentResponse.from(payment),
                OrderDeliveryResponse.from(delivery)
        );
    }

    /**
     * A paid order (PAID or PREPARING — anything up to but not including SHIPPING) requires an
     * external Toss cancel call, which must never happen while holding a DB row lock. This method
     * stays non-transactional and only orchestrates: the not-yet-paid branch is handled entirely in
     * {@link #cancelNonPaidOrder}, and the paid branch is delegated to
     * {@link PaymentService#cancelPaidOrder}, which manages its own short lock/call/lock phases.
     * If payment becomes PAID between request receipt and lock acquisition (a confirm racing this
     * cancel), {@link #cancelNonPaidOrder} signals that via {@link OrderRequiresPaymentCancelException}
     * so we can re-route to the paid path instead of silently failing.
     *
     * <p>Explicitly NOT_SUPPORTED (not just "no annotation") because the class-level
     * {@code @Transactional(readOnly = true)} would otherwise apply here, and if that read-only
     * transaction were active when {@code self.cancelNonPaidOrder} joins it, Hibernate would skip
     * flushing and every write below would be silently discarded.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public OrderCancelResponse cancel(Long memberId, String orderNumber) {
        try {
            return self.cancelNonPaidOrder(memberId, orderNumber);
        } catch (OrderRequiresPaymentCancelException exception) {
            return paymentService.cancelPaidOrder(memberId, orderNumber);
        }
    }

    /** Admin equivalent of {@link #cancel} — same state machine, but looked up by orderNumber alone (no ownership check). */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public OrderCancelResponse adminCancel(String orderNumber, Long adminMemberId, String reason) {
        try {
            return self.adminCancelNonPaidOrder(orderNumber, adminMemberId, reason);
        } catch (OrderRequiresPaymentCancelException exception) {
            return paymentService.adminCancelPaidOrder(orderNumber, adminMemberId, reason);
        }
    }

    @Transactional
    public OrderCancelResponse cancelNonPaidOrder(Long memberId, String orderNumber) {
        Order order = orderRepository.findByOrderNumberAndMemberIdForUpdate(orderNumber, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "Order was not found."));
        return cancelLockedOrder(order, null, "고객 요청에 의한 주문 취소");
    }

    @Transactional
    public OrderCancelResponse adminCancelNonPaidOrder(String orderNumber, Long adminMemberId, String reason) {
        Order order = orderRepository.findByOrderNumberForUpdate(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "Order was not found."));
        String historyReason = reason == null || reason.isBlank() ? "관리자 주문 취소" : "관리자 주문 취소: " + reason;
        return cancelLockedOrder(order, adminMemberId, historyReason);
    }

    /**
     * Shared cancel state machine for an already row-locked order. Routing to the Toss-cancel path
     * is decided by the *payment's* status (PAID), not the order's — a PREPARING order (see
     * {@link #startPreparing}) still has a captured payment and must go through Toss just like a
     * plain PAID order would.
     */
    private OrderCancelResponse cancelLockedOrder(Order order, Long adminMemberId, String reason) {
        if (order.getOrderStatus() == OrderStatus.SHIPPING || order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new InvalidRequestException("ORDER_CANNOT_BE_CANCELED", "Order cannot be canceled in the current status.");
        }

        Payment payment = paymentRepository.findByOrderIdForUpdate(order.getId())
                .orElseThrow(() -> new ResourceNotFoundException("PAYMENT_NOT_FOUND", "Payment was not found."));

        // A payment already CANCELED only ever happens via PaymentService's Toss-cancel path
        // (see cancelPaidOrder/completeCancel) — treat a repeat of that as an idempotent replay
        // rather than the "you already canceled this unpaid order" error below.
        if (payment.getStatus() == PaymentStatus.CANCELED) {
            return OrderCancelResponse.from(order);
        }
        if (order.getOrderStatus() == OrderStatus.CANCELED || order.getOrderStatus() == OrderStatus.EXPIRED) {
            throw new InvalidRequestException("ORDER_ALREADY_CANCELED", "Order is already canceled.");
        }
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new OrderRequiresPaymentCancelException();
        }
        if (payment.getStatus() == PaymentStatus.IN_PROGRESS) {
            throw new InvalidRequestException("PAYMENT_IN_PROGRESS", "Payment is in progress. Please try again shortly.");
        }

        order.cancel();
        deliveryRepository.findByOrderId(order.getId()).ifPresent(Delivery::cancel);
        stockRestorationService.restore(order.getId(), InventoryChangeType.ORDER_CANCEL, reason, adminMemberId);
        couponService.restoreForOrder(order);

        return OrderCancelResponse.from(order);
    }

    /** Pure control-flow signal; no message/stack trace needed. */
    private static final class OrderRequiresPaymentCancelException extends RuntimeException {
        OrderRequiresPaymentCancelException() {
            super(null, null, false, false);
        }
    }

    private Map<Long, List<OrderItem>> orderItemsByOrderId(List<Long> orderIds) {
        if (orderIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<OrderItem>> result = new HashMap<>();
        for (OrderItem orderItem : orderItemRepository.findByOrderIdsOrderByOrderIdAndId(orderIds)) {
            result.computeIfAbsent(orderItem.getOrder().getId(), ignored -> new ArrayList<>()).add(orderItem);
        }
        return result;
    }

    private void validateCartItemIds(List<Long> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            throw new InvalidRequestException("EMPTY_ORDER_ITEMS", "At least one cart item must be selected.");
        }
        if (new HashSet<>(cartItemIds).size() != cartItemIds.size()) {
            throw new InvalidRequestException("INVALID_ORDER_ITEM", "Duplicate cart item IDs are not allowed.");
        }
    }

    private void validateProductForOrder(Product product, int quantity) {
        if (product.getStatus() == ProductStatus.SOLD_OUT || product.getStockQuantity() == 0) {
            throw new InvalidRequestException("PRODUCT_SOLD_OUT", "Product is sold out.");
        }
        if (!product.isPurchasable()) {
            throw new InvalidRequestException("PRODUCT_NOT_AVAILABLE", "Product is not available for purchase.");
        }
        if (!product.hasEnoughStock(quantity)) {
            throw new InvalidRequestException("INSUFFICIENT_STOCK", "Product stock is insufficient.");
        }
    }

    private ShippingAddress resolveShippingAddress(Long memberId, OrderCreateRequest request) {
        String deliveryMessage = normalizeNullable(request.deliveryMessage());
        if (deliveryMessage != null && deliveryMessage.length() > 100) {
            throw new InvalidRequestException("INVALID_ADDRESS", "Delivery message must be 100 characters or less.");
        }

        if (request.addressId() != null) {
            Address address = addressService.findOwnedAddress(memberId, request.addressId());
            return new ShippingAddress(
                    address.getRecipientName(),
                    address.getRecipientPhone(),
                    address.getZipCode(),
                    address.getRoadAddress(),
                    address.getJibunAddress(),
                    address.getDetailAddress(),
                    address.getExtraAddress(),
                    deliveryMessage
            );
        }

        String recipientName = normalizeRequired(request.recipientName(), "INVALID_ADDRESS", "Recipient name is required.");
        String recipientPhone = normalizePhone(request.recipientPhone());
        String zipCode = normalizeZipCode(request.zipCode());
        String roadAddress = normalizeRequired(firstNonBlank(request.roadAddress(), request.address1()), "INVALID_ADDRESS", "Road address is required.");
        String detailAddress = normalizeRequired(firstNonBlank(request.detailAddress(), request.address2()), "INVALID_ADDRESS", "Detail address is required.");

        return new ShippingAddress(
                recipientName,
                recipientPhone,
                zipCode,
                roadAddress,
                normalizeNullable(request.jibunAddress()),
                detailAddress,
                normalizeNullable(request.extraAddress()),
                deliveryMessage
        );
    }

    private String firstNonBlank(String first, String second) {
        String normalizedFirst = normalizeNullable(first);
        return normalizedFirst != null ? normalizedFirst : normalizeNullable(second);
    }

    private String normalizePhone(String phone) {
        String normalized = normalizeRequired(phone, "INVALID_PHONE_NUMBER", "Recipient phone is required.")
                .replaceAll("\\D", "");
        if (!normalized.matches("^01\\d{8,9}$")) {
            throw new InvalidRequestException("INVALID_PHONE_NUMBER", "Recipient phone format is invalid.");
        }
        return normalized;
    }

    private String normalizeZipCode(String zipCode) {
        String normalized = normalizeRequired(zipCode, "INVALID_ZIP_CODE", "Zip code is required.");
        if (!normalized.matches("^\\d{5}$")) {
            throw new InvalidRequestException("INVALID_ZIP_CODE", "Zip code must be 5 digits.");
        }
        return normalized;
    }

    private String normalizeRequired(String value, String code, String message) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new InvalidRequestException(code, message);
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String generateUniqueOrderNumber() {
        for (int i = 0; i < 5; i++) {
            String orderNumber = orderNumberGenerator.generate();
            if (!orderRepository.existsByOrderNumber(orderNumber)) {
                return orderNumber;
            }
        }
        throw new DataIntegrityViolationException("Could not generate unique order number.");
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("MEMBER_NOT_FOUND", "Member was not found."));
    }

    private Order getOwnedOrder(Long memberId, String orderNumber) {
        return orderRepository.findDetailByOrderNumberAndMemberId(orderNumber, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "Order was not found."));
    }

    private record OrderItemDraft(Product product, int quantity, BigDecimal unitPrice) {
    }

    private record ShippingAddress(
            String recipientName,
            String recipientPhone,
            String zipCode,
            String roadAddress,
            String jibunAddress,
            String detailAddress,
            String extraAddress,
            String deliveryMessage
    ) {
    }
}
