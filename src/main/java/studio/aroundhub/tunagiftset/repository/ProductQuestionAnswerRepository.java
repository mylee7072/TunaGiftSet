package studio.aroundhub.tunagiftset.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import studio.aroundhub.tunagiftset.entity.ProductQuestionAnswer;

public interface ProductQuestionAnswerRepository extends JpaRepository<ProductQuestionAnswer, Long> {

    Optional<ProductQuestionAnswer> findByQuestionId(Long questionId);
}
