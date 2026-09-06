package studio.aroundhub.tunagiftset.entity;

import jakarta.persistence.Column;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import studio.aroundhub.tunagiftset.entity.common.BaseTimeEntity;
import studio.aroundhub.tunagiftset.entity.type.DiscountType;
import studio.aroundhub.tunagiftset.entity.type.OrderStatus;

@Entity
@Table(
        name = "orders",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_orders_order_number", columnNames = "order_number")
        },
        indexes = {
                @Index(name = "idx_orders_member_id", columnList = "member_id"),
                @Index(name = "idx_orders_order_status", columnList = "order_status"),
                @Index(name = "idx_orders_ordered_at", columnList = "ordered_at"),
                @Index(name = "idx_orders_status_expires_at", columnList = "order_status, expires_at")
        },
        check = @CheckConstraint(
                name = "ck_orders_non_negative_amounts",
                constraint = "product_amount >= 0 and promotion_discount_amount >= 0 and coupon_discount_amount >= 0 and shipping_fee >= 0 and total_amount >= 0"
        )
)
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, length = 40)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(name = "fk_orders_member"))
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 30)
    private OrderStatus orderStatus = OrderStatus.PAYMENT_PENDING;

    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false, length = 30)
    private String recipientPhone;

    @Column(name = "zip_code", nullable = false, length = 20)
    private String zipCode;

    @Column(nullable = false, length = 255)
    private String address1;

    @Column(length = 255)
    private String address2;

    @Column(name = "jibun_address", length = 255)
    private String jibunAddress;

    @Column(name = "extra_address", length = 255)
    private String extraAddress;

    @Column(name = "delivery_message", length = 100)
    private String deliveryMessage;

    @Column(name = "product_amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal productAmount;

    @Column(name = "shipping_fee", nullable = false, precision = 12, scale = 0)
    private BigDecimal shippingFee;

    @Column(name = "promotion_discount_amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal promotionDiscountAmount = BigDecimal.ZERO;

    @Column(name = "coupon_discount_amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal couponDiscountAmount = BigDecimal.ZERO;

    @Column(name = "coupon_name_snapshot", length = 150)
    private String couponNameSnapshot;

    @Column(name = "coupon_code_snapshot", length = 50)
    private String couponCodeSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "coupon_discount_type_snapshot", length = 30)
    private DiscountType couponDiscountTypeSnapshot;

    @Column(name = "coupon_discount_value_snapshot", precision = 12, scale = 0)
    private BigDecimal couponDiscountValueSnapshot;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal totalAmount;

    @Column(name = "ordered_at", nullable = false)
    private Instant orderedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    protected Order() {
    }

    public Order(
            String orderNumber,
            Member member,
            OrderStatus orderStatus,
            String recipientName,
            String recipientPhone,
            String zipCode,
            String address1,
            String address2,
            String deliveryMessage,
            BigDecimal productAmount,
            BigDecimal shippingFee,
            BigDecimal totalAmount,
            Instant orderedAt
    ) {
        this(
                orderNumber,
                member,
                orderStatus,
                recipientName,
                recipientPhone,
                zipCode,
                address1,
                address2,
                null,
                null,
                deliveryMessage,
                productAmount,
                shippingFee,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null,
                null,
                null,
                totalAmount,
                orderedAt,
                orderedAt == null ? null : orderedAt.plusSeconds(30 * 60)
        );
    }

    public Order(
            String orderNumber,
            Member member,
            OrderStatus orderStatus,
            String recipientName,
            String recipientPhone,
            String zipCode,
            String address1,
            String address2,
            String jibunAddress,
            String extraAddress,
            String deliveryMessage,
            BigDecimal productAmount,
            BigDecimal shippingFee,
            BigDecimal totalAmount,
            Instant orderedAt
    ) {
        this(
                orderNumber,
                member,
                orderStatus,
                recipientName,
                recipientPhone,
                zipCode,
                address1,
                address2,
                jibunAddress,
                extraAddress,
                deliveryMessage,
                productAmount,
                shippingFee,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null,
                null,
                null,
                totalAmount,
                orderedAt,
                orderedAt == null ? null : orderedAt.plusSeconds(30 * 60)
        );
    }

    public Order(
            String orderNumber,
            Member member,
            OrderStatus orderStatus,
            String recipientName,
            String recipientPhone,
            String zipCode,
            String address1,
            String address2,
            String jibunAddress,
            String extraAddress,
            String deliveryMessage,
            BigDecimal productAmount,
            BigDecimal shippingFee,
            BigDecimal promotionDiscountAmount,
            BigDecimal couponDiscountAmount,
            String couponNameSnapshot,
            String couponCodeSnapshot,
            DiscountType couponDiscountTypeSnapshot,
            BigDecimal couponDiscountValueSnapshot,
            BigDecimal totalAmount,
            Instant orderedAt,
            Instant expiresAt
    ) {
        this.orderNumber = orderNumber;
        this.member = member;
        this.orderStatus = orderStatus;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.zipCode = zipCode;
        this.address1 = address1;
        this.address2 = address2;
        this.jibunAddress = jibunAddress;
        this.extraAddress = extraAddress;
        this.deliveryMessage = deliveryMessage;
        this.productAmount = productAmount;
        this.shippingFee = shippingFee;
        this.promotionDiscountAmount = promotionDiscountAmount == null ? BigDecimal.ZERO : promotionDiscountAmount;
        this.couponDiscountAmount = couponDiscountAmount == null ? BigDecimal.ZERO : couponDiscountAmount;
        this.couponNameSnapshot = couponNameSnapshot;
        this.couponCodeSnapshot = couponCodeSnapshot;
        this.couponDiscountTypeSnapshot = couponDiscountTypeSnapshot;
        this.couponDiscountValueSnapshot = couponDiscountValueSnapshot;
        this.totalAmount = totalAmount;
        this.orderedAt = orderedAt;
        this.expiresAt = expiresAt;
    }

    public void applyCouponSnapshot(String name, String code, DiscountType discountType, BigDecimal discountValue, BigDecimal discountAmount) {
        this.couponNameSnapshot = name;
        this.couponCodeSnapshot = code;
        this.couponDiscountTypeSnapshot = discountType;
        this.couponDiscountValueSnapshot = discountValue;
        this.couponDiscountAmount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
    }

    public void cancel() {
        if (this.orderStatus == OrderStatus.CANCELED) {
            throw new IllegalStateException("Order is already canceled.");
        }
        if (this.orderStatus == OrderStatus.SHIPPING
                || this.orderStatus == OrderStatus.DELIVERED
                || this.orderStatus == OrderStatus.EXPIRED) {
            throw new IllegalStateException("Order cannot be canceled.");
        }
        this.orderStatus = OrderStatus.CANCELED;
    }

    public void markPaid() {
        if (this.orderStatus == OrderStatus.PAID) {
            return;
        }
        if (this.orderStatus != OrderStatus.PAYMENT_PENDING) {
            throw new IllegalStateException("Order cannot be paid in the current status.");
        }
        this.orderStatus = OrderStatus.PAID;
    }

    public void expire() {
        if (this.orderStatus == OrderStatus.EXPIRED) {
            return;
        }
        if (this.orderStatus != OrderStatus.PAYMENT_PENDING) {
            throw new IllegalStateException("Only payment pending order can expire.");
        }
        this.orderStatus = OrderStatus.EXPIRED;
    }

    public void startPreparing() {
        if (this.orderStatus == OrderStatus.PREPARING) {
            return;
        }
        if (this.orderStatus != OrderStatus.PAID) {
            throw new IllegalStateException("Only a paid order can start preparing.");
        }
        this.orderStatus = OrderStatus.PREPARING;
    }

    public void ship() {
        if (this.orderStatus == OrderStatus.SHIPPING) {
            return;
        }
        if (this.orderStatus != OrderStatus.PREPARING) {
            throw new IllegalStateException("Only a preparing order can start shipping.");
        }
        this.orderStatus = OrderStatus.SHIPPING;
    }

    public void deliver() {
        if (this.orderStatus == OrderStatus.DELIVERED) {
            return;
        }
        if (this.orderStatus != OrderStatus.SHIPPING) {
            throw new IllegalStateException("Only a shipping order can be delivered.");
        }
        this.orderStatus = OrderStatus.DELIVERED;
    }

    public void changeStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public Long getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public Member getMember() {
        return member;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public String getZipCode() {
        return zipCode;
    }

    public String getAddress1() {
        return address1;
    }

    public String getAddress2() {
        return address2;
    }

    public String getJibunAddress() {
        return jibunAddress;
    }

    public String getExtraAddress() {
        return extraAddress;
    }

    public String getDeliveryMessage() {
        return deliveryMessage;
    }

    public BigDecimal getProductAmount() {
        return productAmount;
    }

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public BigDecimal getPromotionDiscountAmount() {
        return promotionDiscountAmount;
    }

    public BigDecimal getCouponDiscountAmount() {
        return couponDiscountAmount;
    }

    public String getCouponNameSnapshot() {
        return couponNameSnapshot;
    }

    public String getCouponCodeSnapshot() {
        return couponCodeSnapshot;
    }

    public DiscountType getCouponDiscountTypeSnapshot() {
        return couponDiscountTypeSnapshot;
    }

    public BigDecimal getCouponDiscountValueSnapshot() {
        return couponDiscountValueSnapshot;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Instant getOrderedAt() {
        return orderedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
