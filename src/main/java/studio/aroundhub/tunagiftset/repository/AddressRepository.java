package studio.aroundhub.tunagiftset.repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import studio.aroundhub.tunagiftset.entity.Address;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByMemberId(Long memberId);

    List<Address> findAllByMemberIdOrderByDefaultAddressDescCreatedAtDesc(Long memberId);

    Optional<Address> findByIdAndMemberId(Long id, Long memberId);

    Optional<Address> findFirstByMemberIdAndDefaultAddressTrue(Long memberId);

    long countByMemberId(Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select a
            from Address a
            where a.member.id = :memberId
            order by a.defaultAddress desc, a.createdAt desc, a.id desc
            """)
    List<Address> findAllByMemberIdForUpdate(Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select a
            from Address a
            where a.id = :addressId
              and a.member.id = :memberId
            """)
    Optional<Address> findByIdAndMemberIdForUpdate(Long addressId, Long memberId);
}
