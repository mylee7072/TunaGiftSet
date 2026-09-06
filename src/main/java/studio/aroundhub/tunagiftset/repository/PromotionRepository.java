package studio.aroundhub.tunagiftset.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studio.aroundhub.tunagiftset.entity.Promotion;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
}
