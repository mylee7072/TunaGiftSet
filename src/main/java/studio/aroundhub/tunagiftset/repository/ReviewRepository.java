package studio.aroundhub.tunagiftset.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import studio.aroundhub.tunagiftset.entity.Review;
import studio.aroundhub.tunagiftset.entity.type.ReviewStatus;

public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    boolean existsByOrderItemId(Long orderItemId);

    @EntityGraph(attributePaths = {"member", "product", "orderItem"})
    Optional<Review> findByIdAndStatusNot(Long id, ReviewStatus status);

    @EntityGraph(attributePaths = {"member", "product", "orderItem"})
    Page<Review> findAllByProductIdAndStatus(Long productId, ReviewStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"member", "product", "orderItem"})
    Page<Review> findAllByMemberIdAndStatusNot(Long memberId, ReviewStatus status, Pageable pageable);

    @Query("""
            select coalesce(avg(r.rating), 0) as averageRating, count(r) as reviewCount
            from Review r
            where r.product.id = :productId
              and r.status = studio.aroundhub.tunagiftset.entity.type.ReviewStatus.VISIBLE
            """)
    ReviewAggregate aggregateByProductId(Long productId);

    @Query("""
            select r.product.id as productId,
                   coalesce(avg(r.rating), 0) as averageRating,
                   count(r) as reviewCount
            from Review r
            where r.product.id in :productIds
              and r.status = studio.aroundhub.tunagiftset.entity.type.ReviewStatus.VISIBLE
            group by r.product.id
            """)
    List<ProductReviewAggregate> aggregateByProductIds(Collection<Long> productIds);

    interface ReviewAggregate {
        Number getAverageRating();

        long getReviewCount();
    }

    interface ProductReviewAggregate {
        Long getProductId();

        Number getAverageRating();

        long getReviewCount();
    }
}
