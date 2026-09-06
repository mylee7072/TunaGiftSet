package studio.aroundhub.tunagiftset.brand.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studio.aroundhub.tunagiftset.brand.dto.BrandCreateRequest;
import studio.aroundhub.tunagiftset.brand.dto.BrandResponse;
import studio.aroundhub.tunagiftset.brand.dto.BrandUpdateRequest;
import studio.aroundhub.tunagiftset.entity.Brand;
import studio.aroundhub.tunagiftset.exception.DuplicateResourceException;
import studio.aroundhub.tunagiftset.exception.ResourceNotFoundException;
import studio.aroundhub.tunagiftset.repository.BrandRepository;

@Service
@Transactional(readOnly = true)
public class BrandService {

    private final BrandRepository brandRepository;

    public BrandService(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    @Transactional
    public BrandResponse create(BrandCreateRequest request) {
        if (brandRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("BRAND_DUPLICATED", "이미 사용 중인 브랜드 코드입니다.");
        }

        Brand brand = new Brand(request.name(), request.displayName(), request.active());
        return BrandResponse.from(brandRepository.save(brand));
    }

    @Transactional
    public BrandResponse update(Long brandId, BrandUpdateRequest request) {
        Brand brand = getBrand(brandId);

        if (brandRepository.existsByNameAndIdNot(request.name(), brandId)) {
            throw new DuplicateResourceException("BRAND_DUPLICATED", "이미 사용 중인 브랜드 코드입니다.");
        }

        brand.update(request.name(), request.displayName(), request.active());
        return BrandResponse.from(brand);
    }

    public List<BrandResponse> findActiveBrands() {
        return brandRepository.findByActiveTrueOrderByDisplayNameAsc().stream()
                .map(BrandResponse::from)
                .toList();
    }

    /** Admin management view — includes inactive brands, which the public listing hides. */
    public List<BrandResponse> findAllBrands() {
        return brandRepository.findAllByOrderByDisplayNameAsc().stream()
                .map(BrandResponse::from)
                .toList();
    }

    public Brand getBrand(Long brandId) {
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("BRAND_NOT_FOUND", "브랜드를 찾을 수 없습니다."));
    }
}
