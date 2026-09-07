package studio.aroundhub.tunagiftset.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import studio.aroundhub.tunagiftset.entity.common.BaseTimeEntity;
import studio.aroundhub.tunagiftset.entity.type.MemberProvider;
import studio.aroundhub.tunagiftset.entity.type.MemberRole;
import studio.aroundhub.tunagiftset.entity.type.MemberStatus;

@Entity
@Table(
        name = "members",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_members_email", columnNames = "email"),
                // Postgres treats multiple NULLs as distinct, so LOCAL members (provider_id
                // always null) never collide against each other or against this constraint.
                @UniqueConstraint(name = "uk_members_provider_provider_id", columnNames = {"provider", "provider_id"})
        }
)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    // Null for members created via social login (KAKAO/GOOGLE) — they have no local
    // password, so BCryptPasswordEncoder#matches(raw, null) is relied on to always
    // return false rather than throw, which correctly rejects password-based login for them.
    @Column(length = 255)
    private String password;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role = MemberRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status = MemberStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'LOCAL'")
    private MemberProvider provider = MemberProvider.LOCAL;

    @Column(name = "provider_id", length = 100)
    private String providerId;

    protected Member() {
    }

    public Member(String email, String password, String name, String phone) {
        this(email, password, name, phone, MemberRole.USER, MemberStatus.ACTIVE);
    }

    public Member(String email, String password, String name, String phone, MemberRole role, MemberStatus status) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.role = role;
        this.status = status;
        this.provider = MemberProvider.LOCAL;
    }

    public static Member oauth(String email, String name, MemberProvider provider, String providerId) {
        Member member = new Member(email, null, name, null, MemberRole.USER, MemberStatus.ACTIVE);
        member.provider = provider;
        member.providerId = providerId;
        return member;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public MemberRole getRole() {
        return role;
    }

    public MemberStatus getStatus() {
        return status;
    }

    public MemberProvider getProvider() {
        return provider;
    }

    public String getProviderId() {
        return providerId;
    }
}
