package studio.aroundhub.tunagiftset.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import studio.aroundhub.tunagiftset.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByActiveTrueOrderByDisplayOrderAsc();

    List<Category> findAllByOrderByDisplayOrderAsc();

    List<Category> findByParentIdOrderByDisplayOrderAsc(Long parentId);
}
