package studio.aroundhub.tunagiftset.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import studio.aroundhub.tunagiftset.entity.Brand;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    Optional<Brand> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    List<Brand> findByActiveTrueOrderByDisplayNameAsc();

    List<Brand> findAllByOrderByDisplayNameAsc();
}
