package studio.aroundhub.tunagiftset.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import studio.aroundhub.tunagiftset.entity.Member;
import studio.aroundhub.tunagiftset.entity.type.MemberProvider;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<Member> findByProviderAndProviderId(MemberProvider provider, String providerId);
}
