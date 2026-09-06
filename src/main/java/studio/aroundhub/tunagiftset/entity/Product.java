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
import jakarta.persistence.Version;
import java.math.BigDecimal;
import studio.aroundhub.tunagiftset.entity.common.BaseTimeEntity;
import studio.aroundhub.tunagiftset.entity.type.ProductStatus;

@Entity
@Table(
        name = "products",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_products_product_code", columnNames = "product_code")
        },
        indexes = {
                @Index(name = "idx_products_brand_id", columnList = "brand_id"),
                @Index(name = "idx_products_category_id", columnList = "category_id"),
                @Index(name = "idx_products_status", columnList = "status"),
                @Index(name = "idx_products_featured", columnList = "featured")
        },
        check = @CheckConstraint(
                name = "ck_products_non_negative_amounts_and_stock",
                constraint = "original_price >= 0 and sale_price >= 0 and stock_quantity >= 0"
        )
)
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false, foreignKey = @ForeignKey(name = "fk_products_brand"))
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false, foreignKey = @ForeignKey(name = "fk_products_category"))
    private Category category;

    @Column(name = "product_code", nullable = false, length = 50)
    private String productCode;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "original_price", nullable = false, precision = 12, scale = 0)
    private BigDecimal originalPrice;

    @Column(name = "sale_price", nullable = false, precision = 12, scale = 0)
    private BigDecimal salePrice;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductStatus status = ProductStatus.HIDDEN;

    @Column(nullable = false)
    private boolean featured = false;

    @Version
    @Column(nullable = false)
    private Long version;

    protected Product() {
    }

    public Product(
            Brand brand,
            Category category,
            String productCode,
            String name,
            String shortDescription,
            String description,
            BigDecimal originalPrice,
            BigDecimal salePrice,
            int stockQuantity,
            ProductStatus status,
            boolean featured
    ) {
        this.brand = brand;
        this.category = category;
        this.productCode = productCode;
        this.name = name;
        this.shortDescription = shortDescription;
        this.description = description;
        this.originalPrice = originalPrice;
        this.salePrice = salePrice;
        this.stockQuantity = stockQuantity;
        this.status = status;
        this.featured = featured;
    }

    public void update(
            Brand brand,
            Category category,
            String productCode,
            String name,
            String shortDescription,
            String description,
            BigDecimal originalPrice,
            BigDecimal salePrice,
            int stockQuantity,
            ProductStatus status,
            boolean featured
    ) {
        this.brand = brand;
        this.category = category;
        this.productCode = productCode;
        this.name = name;
        this.shortDescription = shortDescription;
        this.description = description;
        this.originalPrice = originalPrice;
        this.salePrice = salePrice;
        this.stockQuantity = stockQuantity;
        this.status = status;
        this.featured = featured;
    }

    public void changeStatus(ProductStatus status) {
        this.status = status;
    }

    public boolean isPurchasable() {
        return status == ProductStatus.ACTIVE && stockQuantity > 0;
    }

    public boolean hasEnoughStock(int quantity) {
        return stockQuantity >= quantity;
    }

    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Stock quantity must be positive.");
        }
        if (!hasEnoughStock(quantity)) {
            throw new IllegalStateException("Insufficient stock.");
        }
        this.stockQuantity -= quantity;
    }

    public void increaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Stock quantity must be positive.");
        }
        this.stockQuantity += quantity;
    }

    public Long getId() {
        return id;
    }

    public Brand getBrand() {
        return brand;
    }

    public Category getCategory() {
        return category;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getName() {
        return name;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public boolean isFeatured() {
        return featured;
    }

    public Long getVersion() {
        return version;
    }
}
