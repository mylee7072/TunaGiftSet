package studio.aroundhub.tunagiftset.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import studio.aroundhub.tunagiftset.entity.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByMemberId(Long memberId);

    boolean existsByMemberId(Long memberId);
}
