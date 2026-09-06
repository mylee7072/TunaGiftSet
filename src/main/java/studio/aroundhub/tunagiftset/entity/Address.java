package studio.aroundhub.tunagiftset.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import studio.aroundhub.tunagiftset.entity.common.BaseTimeEntity;

@Entity
@Table(
        name = "addresses",
        indexes = {
                @Index(name = "idx_addresses_member_id", columnList = "member_id"),
                @Index(name = "idx_addresses_member_default", columnList = "member_id,is_default")
        }
)
public class Address extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(name = "fk_addresses_member"))
    private Member member;

    @Column(name = "address_name", length = 100)
    private String addressName;

    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false, length = 30)
    private String recipientPhone;

    @Column(name = "zip_code", nullable = false, length = 20)
    private String zipCode;

    @Column(name = "road_address", nullable = false, length = 255)
    private String roadAddress;

    @Column(name = "jibun_address", length = 255)
    private String jibunAddress;

    @Column(name = "detail_address", nullable = false, length = 255)
    private String detailAddress;

    @Column(name = "extra_address", length = 255)
    private String extraAddress;

    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress = false;

    protected Address() {
    }

    public Address(
            Member member,
            String addressName,
            String recipientName,
            String recipientPhone,
            String zipCode,
            String roadAddress,
            String jibunAddress,
            String detailAddress,
            String extraAddress,
            boolean defaultAddress
    ) {
        this.member = member;
        this.addressName = addressName;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.zipCode = zipCode;
        this.roadAddress = roadAddress;
        this.jibunAddress = jibunAddress;
        this.detailAddress = detailAddress;
        this.extraAddress = extraAddress;
        this.defaultAddress = defaultAddress;
    }

    public void update(
            String addressName,
            String recipientName,
            String recipientPhone,
            String zipCode,
            String roadAddress,
            String jibunAddress,
            String detailAddress,
            String extraAddress
    ) {
        this.addressName = addressName;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.zipCode = zipCode;
        this.roadAddress = roadAddress;
        this.jibunAddress = jibunAddress;
        this.detailAddress = detailAddress;
        this.extraAddress = extraAddress;
    }

    public void markDefault() {
        this.defaultAddress = true;
    }

    public void unmarkDefault() {
        this.defaultAddress = false;
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public String getAddressName() {
        return addressName;
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

    public String getRoadAddress() {
        return roadAddress;
    }

    public String getJibunAddress() {
        return jibunAddress;
    }

    public String getDetailAddress() {
        return detailAddress;
    }

    public String getExtraAddress() {
        return extraAddress;
    }

    public boolean isDefaultAddress() {
        return defaultAddress;
    }
}
