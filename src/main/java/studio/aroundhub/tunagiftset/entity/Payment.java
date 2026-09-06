package studio.aroundhub.tunagiftset.entity;

import jakarta.persistence.CheckConstraint;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import studio.aroundhub.tunagiftset.entity.common.BaseTimeEntity;
import studio.aroundhub.tunagiftset.entity.type.PaymentMethod;
import studio.aroundhub.tunagiftset.entity.type.PaymentStatus;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payments_order_id", columnNames = "order_id"),
                @UniqueConstraint(name = "uk_payments_payment_key", columnNames = "payment_key")
        },
        indexes = {
                @Index(name = "idx_payments_status", columnList = "status"),
                @Index(name = "idx_payments_payment_key", columnList = "payment_key")
        },
        check = @CheckConstraint(
                name = "ck_payments_amounts_non_negative",
                constraint = "amount >= 0 and approved_amount >= 0"
        )
)
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_payments_order"))
    private Order order;

    @Column(name = "payment_key", length = 200)
    private String paymentKey;

    @Column(nullable = false, length = 30)
    private String provider = "TOSS";

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 50)
    private PaymentMethod paymentMethod;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal amount;

    @Column(name = "approved_amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal approvedAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 10)
    private String currency = "KRW";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status = PaymentStatus.READY;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    @Version
    @Column(nullable = false)
    private Long version;

    protected Payment() {
    }

    public Payment(Order order, BigDecimal amount, PaymentStatus status) {
        this.order = order;
        this.amount = amount;
        this.status = status;
    }

    public void startConfirm(String paymentKey) {
        if (status == PaymentStatus.PAID) {
            throw new IllegalStateException("Payment is already completed.");
        }
        if (status == PaymentStatus.CANCELED) {
            throw new IllegalStateException("Payment is canceled.");
        }
        if (status == PaymentStatus.IN_PROGRESS && this.paymentKey != null && !this.paymentKey.equals(paymentKey)) {
            throw new IllegalStateException("Payment confirmation is already in progress with a different payment key.");
        }
        this.paymentKey = paymentKey;
        this.status = PaymentStatus.IN_PROGRESS;
        this.failureCode = null;
        this.failureMessage = null;
    }

    public void markPaid(String paymentKey, PaymentMethod paymentMethod, BigDecimal approvedAmount, String currency, Instant approvedAt) {
        if (status == PaymentStatus.PAID) {
            return;
        }
        if (status == PaymentStatus.CANCELED) {
            throw new IllegalStateException("Canceled payment cannot be paid.");
        }
        if (this.paymentKey != null && !this.paymentKey.equals(paymentKey)) {
            throw new IllegalStateException("Payment key does not match.");
        }
        this.paymentKey = paymentKey;
        this.paymentMethod = paymentMethod;
        this.approvedAmount = approvedAmount;
        this.currency = currency == null || currency.isBlank() ? "KRW" : currency;
        this.status = PaymentStatus.PAID;
        this.paidAt = approvedAt;
        this.failureCode = null;
        this.failureMessage = null;
    }

    public void markFailed(String failureCode, String failureMessage) {
        if (status == PaymentStatus.PAID || status == PaymentStatus.CANCELED) {
            return;
        }
        this.status = PaymentStatus.FAILED;
        this.failureCode = trim(failureCode, 100);
        this.failureMessage = trim(failureMessage, 500);
    }

    public void markCanceled(Instant canceledAt) {
        if (status == PaymentStatus.CANCELED) {
            return;
        }
        if (status != PaymentStatus.PAID) {
            throw new IllegalStateException("Only paid payment can be canceled.");
        }
        this.status = PaymentStatus.CANCELED;
        this.canceledAt = canceledAt;
    }

    private String trim(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public String getPaymentKey() {
        return paymentKey;
    }

    public String getProvider() {
        return provider;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public Instant getCanceledAt() {
        return canceledAt;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public Long getVersion() {
        return version;
    }
}
