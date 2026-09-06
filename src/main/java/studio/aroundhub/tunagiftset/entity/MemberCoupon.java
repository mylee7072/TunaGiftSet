package studio.aroundhub.tunagiftset.entity;

import jakarta.persistence.Column;
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
import jakarta.persistence.Version;
import java.time.Instant;
import studio.aroundhub.tunagiftset.entity.common.BaseTimeEntity;
import studio.aroundhub.tunagiftset.entity.type.MemberCouponStatus;

@Entity
@Table(
        name = "member_coupons",
        indexes = {
                @Index(name = "idx_member_coupons_member_id", columnList = "member_id"),
                @Index(name = "idx_member_coupons_coupon_id", columnList = "coupon_id"),
                @Index(name = "idx_member_coupons_status", columnList = "status"),
                @Index(name = "idx_member_coupons_reserved_order_id", columnList = "reserved_order_id"),
                @Index(name = "idx_member_coupons_used_order_id", columnList = "used_order_id")
        }
)
public class MemberCoupon extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(name = "fk_member_coupons_member"))
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false, foreignKey = @ForeignKey(name = "fk_member_coupons_coupon"))
    private Coupon coupon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MemberCouponStatus status = MemberCouponStatus.AVAILABLE;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "reserved_at")
    private Instant reservedAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserved_order_id", foreignKey = @ForeignKey(name = "fk_member_coupons_reserved_order"))
    private Order reservedOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "used_order_id", foreignKey = @ForeignKey(name = "fk_member_coupons_used_order"))
    private Order usedOrder;

    @Version
    @Column(nullable = false)
    private Long version;

    protected MemberCoupon() {
    }

    public MemberCoupon(Member member, Coupon coupon, Instant issuedAt) {
        this.member = member;
        this.coupon = coupon;
        this.issuedAt = issuedAt;
    }

    public void reserve(Order order, Instant reservedAt) {
        if (status != MemberCouponStatus.AVAILABLE) {
            throw new IllegalStateException("Coupon is not available.");
        }
        this.status = MemberCouponStatus.RESERVED;
        this.reservedOrder = order;
        this.reservedAt = reservedAt;
    }

    public void markUsed(Order order, Instant usedAt) {
        if (status == MemberCouponStatus.USED) {
            return;
        }
        if (status != MemberCouponStatus.RESERVED || reservedOrder == null || !reservedOrder.getId().equals(order.getId())) {
            throw new IllegalStateException("Coupon is not reserved for this order.");
        }
        this.status = MemberCouponStatus.USED;
        this.usedOrder = order;
        this.usedAt = usedAt;
    }

    public void restoreFromOrder(Order order) {
        if (status == MemberCouponStatus.AVAILABLE) {
            return;
        }
        boolean matchesReserved = reservedOrder != null && reservedOrder.getId().equals(order.getId());
        boolean matchesUsed = usedOrder != null && usedOrder.getId().equals(order.getId());
        if (!matchesReserved && !matchesUsed) {
            return;
        }
        this.status = MemberCouponStatus.AVAILABLE;
        this.reservedOrder = null;
        this.usedOrder = null;
        this.reservedAt = null;
        this.usedAt = null;
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public Coupon getCoupon() {
        return coupon;
    }

    public MemberCouponStatus getStatus() {
        return status;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getReservedAt() {
        return reservedAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Order getReservedOrder() {
        return reservedOrder;
    }

    public Order getUsedOrder() {
        return usedOrder;
    }
}
