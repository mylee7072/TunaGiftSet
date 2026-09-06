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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import studio.aroundhub.tunagiftset.entity.common.BaseTimeEntity;
import studio.aroundhub.tunagiftset.entity.type.DeliveryStatus;

@Entity
@Table(
        name = "deliveries",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_deliveries_order_id", columnNames = "order_id")
        }
)
public class Delivery extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_deliveries_order"))
    private Order order;

    @Column(length = 100)
    private String carrier;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeliveryStatus status = DeliveryStatus.READY;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    protected Delivery() {
    }

    public Delivery(Order order, DeliveryStatus status) {
        this.order = order;
        this.status = status;
    }

    public void startPreparing() {
        if (status == DeliveryStatus.PREPARING) {
            return;
        }
        if (status != DeliveryStatus.READY) {
            throw new IllegalStateException("Only a ready delivery can start preparing.");
        }
        this.status = DeliveryStatus.PREPARING;
    }

    public void registerTracking(String carrier, String trackingNumber) {
        if (status == DeliveryStatus.SHIPPING || status == DeliveryStatus.DELIVERED) {
            throw new IllegalStateException("Tracking information cannot be changed after shipping has started.");
        }
        this.carrier = carrier;
        this.trackingNumber = trackingNumber;
    }

    public void ship(Instant shippedAt) {
        if (status == DeliveryStatus.SHIPPING) {
            return;
        }
        if (status != DeliveryStatus.READY && status != DeliveryStatus.PREPARING) {
            throw new IllegalStateException("Delivery cannot start shipping in the current status.");
        }
        if (trackingNumber == null || trackingNumber.isBlank()) {
            throw new IllegalStateException("Tracking number is required before shipping.");
        }
        this.status = DeliveryStatus.SHIPPING;
        this.shippedAt = shippedAt;
    }

    public void deliver(Instant deliveredAt) {
        if (status == DeliveryStatus.DELIVERED) {
            return;
        }
        if (status != DeliveryStatus.SHIPPING) {
            throw new IllegalStateException("Only a shipping delivery can be marked delivered.");
        }
        this.status = DeliveryStatus.DELIVERED;
        this.deliveredAt = deliveredAt;
    }

    public void cancel() {
        if (status == DeliveryStatus.CANCELED) {
            return;
        }
        if (status == DeliveryStatus.SHIPPING || status == DeliveryStatus.DELIVERED) {
            throw new IllegalStateException("Delivery cannot be canceled in the current status.");
        }
        this.status = DeliveryStatus.CANCELED;
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public String getCarrier() {
        return carrier;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public Instant getShippedAt() {
        return shippedAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }
}
