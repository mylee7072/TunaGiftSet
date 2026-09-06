package studio.aroundhub.tunagiftset.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import studio.aroundhub.tunagiftset.entity.ProductImage;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductIdOrderByDisplayOrderAsc(Long productId);

    Optional<ProductImage> findByIdAndProductId(Long id, Long productId);

    long countByProductId(Long productId);

    boolean existsByProductIdAndImageType(Long productId, studio.aroundhub.tunagiftset.entity.type.ProductImageType imageType);

    @Query("""
            select pi.product.id as productId, pi.objectKey as objectKey, pi.imageUrl as imageUrl
            from ProductImage pi
            where pi.imageType = studio.aroundhub.tunagiftset.entity.type.ProductImageType.MAIN
              and pi.product.id in :productIds
              and pi.displayOrder = (
                  select min(pi2.displayOrder)
                  from ProductImage pi2
                  where pi2.product.id = pi.product.id
                    and pi2.imageType = studio.aroundhub.tunagiftset.entity.type.ProductImageType.MAIN
              )
            """)
    List<ProductThumbnailProjection> findMainThumbnailsByProductIds(List<Long> productIds);

    interface ProductThumbnailProjection {
        Long getProductId();

        String getObjectKey();

        String getImageUrl();
    }
}
