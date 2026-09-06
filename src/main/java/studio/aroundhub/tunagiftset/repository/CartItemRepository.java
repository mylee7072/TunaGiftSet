package studio.aroundhub.tunagiftset.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import studio.aroundhub.tunagiftset.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    @Query("""
            select ci
            from CartItem ci
            join fetch ci.product p
            join fetch p.brand
            join fetch p.category
            where ci.cart.id = :cartId
            order by ci.id asc
            """)
    List<CartItem> findAllWithProductByCartId(Long cartId);

    Optional<CartItem> findByIdAndCartId(Long id, Long cartId);

    List<CartItem> findByIdInAndCartId(Collection<Long> ids, Long cartId);

    long countByCartId(Long cartId);

    void deleteByCartId(Long cartId);
}
