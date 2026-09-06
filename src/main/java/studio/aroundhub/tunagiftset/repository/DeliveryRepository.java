package studio.aroundhub.tunagiftset.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import studio.aroundhub.tunagiftset.entity.Delivery;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    Optional<Delivery> findByOrderId(Long orderId);

    List<Delivery> findByOrderIdIn(List<Long> orderIds);
}
