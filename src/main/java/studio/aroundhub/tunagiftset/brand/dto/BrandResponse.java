package studio.aroundhub.tunagiftset.brand.dto;

import studio.aroundhub.tunagiftset.entity.Brand;

public record BrandResponse(
        Long id,
        String name,
        String displayName,
        boolean active
) {
    public static BrandResponse from(Brand brand) {
        return new BrandResponse(
                brand.getId(),
                brand.getName(),
                brand.getDisplayName(),
                brand.isActive()
        );
    }
}
