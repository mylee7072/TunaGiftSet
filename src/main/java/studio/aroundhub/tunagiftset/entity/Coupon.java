package studio.aroundhub.tunagiftset.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import studio.aroundhub.tunagiftset.entity.common.BaseTimeEntity;
import studio.aroundhub.tunagiftset.entity.type.CouponStatus;
import studio.aroundhub.tunagiftset.entity.type.DiscountType;

@Entity
@Table(
        name = "coupons",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_coupons_code", columnNames = "code")
        },
        indexes = {
                @Index(name = "idx_coupons_status", columnList = "status"),
                @Index(name = "idx_coupons_valid_period", columnList = "valid_from, valid_until")
        }
)
public class Coupon extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 30)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 12, scale = 0)
    private BigDecimal discountValue;

    @Column(name = "minimum_order_amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal minimumOrderAmount;

    @Column(name = "maximum_discount_amount", precision = 12, scale = 0)
    private BigDecimal maximumDiscountAmount;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CouponStatus status = CouponStatus.ACTIVE;

    @Column(name = "total_issue_limit")
    private Integer totalIssueLimit;

    @Column(name = "per_member_limit", nullable = false)
    private int perMemberLimit = 1;

    @Version
    @Column(nullable = false)
    private Long version;

    protected Coupon() {
    }

    public Coupon(
            String name,
            String code,
            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal minimumOrderAmount,
            BigDecimal maximumDiscountAmount,
            Instant validFrom,
            Instant validUntil,
            CouponStatus status,
            Integer totalIssueLimit,
            int perMemberLimit
    ) {
        this.name = name;
        this.code = code;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minimumOrderAmount = minimumOrderAmount;
        this.maximumDiscountAmount = maximumDiscountAmount;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.status = status;
        this.totalIssueLimit = totalIssueLimit;
        this.perMemberLimit = perMemberLimit;
    }

    public void update(
            String name,
            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal minimumOrderAmount,
            BigDecimal maximumDiscountAmount,
            Instant validFrom,
            Instant validUntil,
            CouponStatus status,
            Integer totalIssueLimit,
            int perMemberLimit
    ) {
        this.name = name;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minimumOrderAmount = minimumOrderAmount;
        this.maximumDiscountAmount = maximumDiscountAmount;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.status = status;
        this.totalIssueLimit = totalIssueLimit;
        this.perMemberLimit = perMemberLimit;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public BigDecimal getMinimumOrderAmount() {
        return minimumOrderAmount;
    }

    public BigDecimal getMaximumDiscountAmount() {
        return maximumDiscountAmount;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

    public CouponStatus getStatus() {
        return status;
    }

    public Integer getTotalIssueLimit() {
        return totalIssueLimit;
    }

    public int getPerMemberLimit() {
        return perMemberLimit;
    }
}
