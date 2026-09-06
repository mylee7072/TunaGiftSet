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
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.EntityListeners;
import studio.aroundhub.tunagiftset.entity.type.InventoryChangeType;

/**
 * Append-only ledger of every stock quantity change. Nothing here is ever updated
 * or deleted — it exists purely so operators can answer "why is this stock number
 * what it is" after the fact (see InventoryService / StockRestorationService).
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "inventory_histories",
        indexes = {
                @Index(name = "idx_inventory_histories_product_id_created_at", columnList = "product_id, created_at")
        }
)
public class InventoryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_inventory_histories_product"))
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 30)
    private InventoryChangeType changeType;

    @Column(name = "quantity_before", nullable = false)
    private int quantityBefore;

    // Signed: positive for an increase, negative for a decrease. quantityAfter is
    // always quantityBefore + changeQuantity, so the three fields self-check.
    @Column(name = "change_quantity", nullable = false)
    private int changeQuantity;

    @Column(name = "quantity_after", nullable = false)
    private int quantityAfter;

    @Column(length = 500)
    private String reason;

    @Column(name = "reference_type", length = 30)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    // Null for changes not initiated by an admin (order placement/cancellation,
    // scheduler expiry) — non-null only for ADMIN_INCREASE/ADMIN_DECREASE.
    @Column(name = "admin_member_id")
    private Long adminMemberId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected InventoryHistory() {
    }

    public InventoryHistory(
            Product product,
            InventoryChangeType changeType,
            int quantityBefore,
            int changeQuantity,
            String reason,
            String referenceType,
            Long referenceId,
            Long adminMemberId
    ) {
        this.product = product;
        this.changeType = changeType;
        this.quantityBefore = quantityBefore;
        this.changeQuantity = changeQuantity;
        this.quantityAfter = quantityBefore + changeQuantity;
        this.reason = reason;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.adminMemberId = adminMemberId;
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public InventoryChangeType getChangeType() {
        return changeType;
    }

    public int getQuantityBefore() {
        return quantityBefore;
    }

    public int getChangeQuantity() {
        return changeQuantity;
    }

    public int getQuantityAfter() {
        return quantityAfter;
    }

    public String getReason() {
        return reason;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public Long getAdminMemberId() {
        return adminMemberId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
