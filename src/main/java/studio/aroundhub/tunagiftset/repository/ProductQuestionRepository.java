package studio.aroundhub.tunagiftset.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import studio.aroundhub.tunagiftset.entity.ProductQuestion;
import studio.aroundhub.tunagiftset.entity.type.QuestionStatus;

public interface ProductQuestionRepository extends JpaRepository<ProductQuestion, Long>, JpaSpecificationExecutor<ProductQuestion> {

    @EntityGraph(attributePaths = {"member", "product", "answer", "answer.adminMember"})
    Optional<ProductQuestion> findByIdAndStatusNot(Long id, QuestionStatus status);

    @EntityGraph(attributePaths = {"member", "product", "answer", "answer.adminMember"})
    Page<ProductQuestion> findAllByProductIdAndStatusNot(Long productId, QuestionStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"member", "product", "answer", "answer.adminMember"})
    Page<ProductQuestion> findAllByMemberIdAndStatusNot(Long memberId, QuestionStatus status, Pageable pageable);
}
