package studio.aroundhub.tunagiftset.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import studio.aroundhub.tunagiftset.entity.Wishlist;
import studio.aroundhub.tunagiftset.entity.type.ProductStatus;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    boolean existsByMemberIdAndProductId(Long memberId, Long productId);

    Optional<Wishlist> findByMemberIdAndProductId(Long memberId, Long productId);

    long countByProductId(Long productId);

    @Query("""
            select w.product.id
            from Wishlist w
            where w.member.id = :memberId
              and w.product.id in :productIds
            """)
    List<Long> findWishlistedProductIds(Long memberId, Collection<Long> productIds);

    @Query(
            value = """
            select w
            from Wishlist w
            join fetch w.product p
            join fetch p.brand
            join fetch p.category
            where w.member.id = :memberId
              and p.status <> :excludedStatus
            """,
            countQuery = """
            select count(w)
            from Wishlist w
            join w.product p
            where w.member.id = :memberId
              and p.status <> :excludedStatus
            """
    )
    Page<Wishlist> findPageByMemberIdExcludingProductStatus(Long memberId, ProductStatus excludedStatus, Pageable pageable);

    @Query("""
            select w.product.id as productId, count(w.id) as wishlistCount
            from Wishlist w
            where w.product.id in :productIds
            group by w.product.id
            """)
    List<WishlistCountProjection> countByProductIds(Collection<Long> productIds);

    interface WishlistCountProjection {
        Long getProductId();

        long getWishlistCount();
    }
}
