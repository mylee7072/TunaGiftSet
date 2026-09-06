package studio.aroundhub.tunagiftset.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import studio.aroundhub.tunagiftset.entity.MemberCoupon;
import studio.aroundhub.tunagiftset.entity.type.MemberCouponStatus;

public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long> {

    long countByCouponId(Long couponId);

    long countByMemberIdAndCouponId(Long memberId, Long couponId);

    @Query("""
            select mc
            from MemberCoupon mc
            join fetch mc.coupon
            where mc.member.id = :memberId
            order by mc.issuedAt desc, mc.id desc
            """)
    Page<MemberCoupon> findPageByMemberId(Long memberId, Pageable pageable);

    @Query("""
            select mc
            from MemberCoupon mc
            join fetch mc.coupon
            where mc.member.id = :memberId
              and mc.status = :status
            order by mc.issuedAt desc, mc.id desc
            """)
    List<MemberCoupon> findByMemberIdAndStatus(Long memberId, MemberCouponStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select mc
            from MemberCoupon mc
            join fetch mc.coupon
            where mc.id = :memberCouponId
              and mc.member.id = :memberId
            """)
    Optional<MemberCoupon> findByIdAndMemberIdForUpdate(Long memberCouponId, Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select mc
            from MemberCoupon mc
            join fetch mc.coupon
            where mc.reservedOrder.id = :orderId
               or mc.usedOrder.id = :orderId
            """)
    Optional<MemberCoupon> findByOrderIdForUpdate(Long orderId);
}
