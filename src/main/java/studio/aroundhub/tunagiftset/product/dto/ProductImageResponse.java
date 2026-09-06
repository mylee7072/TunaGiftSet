package studio.aroundhub.tunagiftset.product.dto;

import studio.aroundhub.tunagiftset.entity.ProductImage;
import studio.aroundhub.tunagiftset.entity.type.ProductImageType;

public record ProductImageResponse(
        Long id,
        String imageUrl,
        ProductImageType imageType,
        int displayOrder,
        String originalFilename,
        String contentType,
        Long fileSize
) {
    public static ProductImageResponse from(ProductImage image) {
        return from(image, image.getImageUrl());
    }

    public static ProductImageResponse from(ProductImage image, String imageUrl) {
        return new ProductImageResponse(
                image.getId(),
                imageUrl,
                image.getImageType(),
                image.getDisplayOrder(),
                image.getOriginalFilename(),
                image.getContentType(),
                image.getFileSize()
        );
    }
}
