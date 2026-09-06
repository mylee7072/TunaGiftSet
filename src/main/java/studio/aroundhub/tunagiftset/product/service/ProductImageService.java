package studio.aroundhub.tunagiftset.product.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import studio.aroundhub.tunagiftset.entity.Product;
import studio.aroundhub.tunagiftset.entity.ProductImage;
import studio.aroundhub.tunagiftset.entity.type.ProductImageType;
import studio.aroundhub.tunagiftset.exception.InvalidRequestException;
import studio.aroundhub.tunagiftset.exception.ResourceNotFoundException;
import studio.aroundhub.tunagiftset.product.dto.ProductImageOrderRequest;
import studio.aroundhub.tunagiftset.product.dto.ProductImageResponse;
import studio.aroundhub.tunagiftset.product.dto.ProductImageUploadRequest;
import studio.aroundhub.tunagiftset.repository.ProductImageRepository;
import studio.aroundhub.tunagiftset.repository.ProductRepository;
import studio.aroundhub.tunagiftset.storage.ObjectStorageService;
import studio.aroundhub.tunagiftset.storage.StorageProperties;
import studio.aroundhub.tunagiftset.storage.StoredFile;

@Service
public class ProductImageService {

    private static final Logger log = LoggerFactory.getLogger(ProductImageService.class);
    private static final Map<String, String> EXTENSIONS_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ObjectStorageService objectStorageService;
    private final StorageProperties storageProperties;
    private final ProductImageService self;

    public ProductImageService(
            ProductRepository productRepository,
            ProductImageRepository productImageRepository,
            ObjectStorageService objectStorageService,
            StorageProperties storageProperties,
            @Lazy ProductImageService self
    ) {
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.objectStorageService = objectStorageService;
        this.storageProperties = storageProperties;
        this.self = self;
    }

    public ProductImageResponse upload(Long productId, ProductImageUploadRequest request) {
        Product product = getProduct(productId);
        MultipartFile file = request.file();
        validateUpload(productId, file);

        String contentType = normalizeContentType(file.getContentType());
        String objectKey = createObjectKey(productId, contentType);
        StoredFile storedFile;

        try (InputStream inputStream = file.getInputStream()) {
            storedFile = objectStorageService.upload(objectKey, inputStream, file.getSize(), contentType);
        } catch (IOException exception) {
            throw new InvalidRequestException("IMAGE_UPLOAD_FAILED", "이미지 업로드 중 오류가 발생했습니다.");
        }

        try {
            ProductImage image = self.saveImage(product, file, request, storedFile);
            log.info("Product image uploaded. productId={} imageId={} objectKey={}", productId, image.getId(), image.getObjectKey());
            return ProductImageResponse.from(image, objectStorageService.publicUrl(image.getObjectKey()));
        } catch (RuntimeException exception) {
            cleanupUploadedObject(objectKey);
            throw exception;
        }
    }

    @Transactional
    public ProductImage saveImage(Product product, MultipartFile file, ProductImageUploadRequest request, StoredFile storedFile) {
        boolean hasImages = productImageRepository.countByProductId(product.getId()) > 0;
        ProductImageType imageType = request.imageType() == null ? ProductImageType.DETAIL : request.imageType();
        if (!hasImages) {
            imageType = ProductImageType.MAIN;
        }
        if (imageType == ProductImageType.MAIN) {
            demoteCurrentMain(product.getId());
        }

        int displayOrder = request.displayOrder() == null ? nextDisplayOrder(product.getId()) : request.displayOrder();
        ProductImage image = productImageRepository.save(new ProductImage(
                product,
                storedFile.objectKey(),
                storedFile.publicUrl(),
                sanitizeFilename(file.getOriginalFilename()),
                storedFile.contentType(),
                storedFile.size(),
                imageType,
                displayOrder
        ));
        return image;
    }

    @Transactional
    public ProductImageResponse setMain(Long productId, Long imageId) {
        getProduct(productId);
        ProductImage image = getOwnedImage(productId, imageId);
        demoteCurrentMain(productId);
        image.markMain();
        return ProductImageResponse.from(image, resolveImageUrl(image));
    }

    @Transactional
    public List<ProductImageResponse> reorder(Long productId, ProductImageOrderRequest request) {
        getProduct(productId);
        List<ProductImage> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId);
        Set<Long> requestedIds = Set.copyOf(request.imageIds());
        if (requestedIds.size() != request.imageIds().size() || requestedIds.size() != images.size()) {
            throw new InvalidRequestException("INVALID_IMAGE_ORDER", "이미지 순서 정보가 올바르지 않습니다.");
        }

        Map<Long, ProductImage> imageById = images.stream()
                .collect(java.util.stream.Collectors.toMap(ProductImage::getId, image -> image));
        for (int i = 0; i < request.imageIds().size(); i++) {
            ProductImage image = imageById.get(request.imageIds().get(i));
            if (image == null) {
                throw new InvalidRequestException("IMAGE_NOT_BELONG_TO_PRODUCT", "해당 상품의 이미지가 아닙니다.");
            }
            image.changeDisplayOrder(i);
        }

        return productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId).stream()
                .sorted(Comparator.comparingInt(ProductImage::getDisplayOrder).thenComparing(ProductImage::getId))
                .map(image -> ProductImageResponse.from(image, resolveImageUrl(image)))
                .toList();
    }

    @Transactional
    public void delete(Long productId, Long imageId) {
        getProduct(productId);
        ProductImage image = getOwnedImage(productId, imageId);
        String objectKey = image.getObjectKey();
        boolean wasMain = image.isMain();

        productImageRepository.delete(image);
        productImageRepository.flush();

        if (wasMain) {
            promoteNextMain(productId);
        }

        scheduleStorageDeleteAfterCommit(productId, imageId, objectKey);
    }

    @Transactional(readOnly = true)
    public List<ProductImageResponse> findImages(Long productId) {
        getProduct(productId);
        return productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId).stream()
                .map(image -> ProductImageResponse.from(image, resolveImageUrl(image)))
                .toList();
    }

    private void validateUpload(Long productId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("EMPTY_IMAGE_FILE", "이미지 파일이 비어 있습니다.");
        }
        if (file.getSize() > storageProperties.productImage().maxFileSizeBytes()) {
            throw new InvalidRequestException("IMAGE_FILE_TOO_LARGE", "이미지 파일 크기가 제한을 초과했습니다.");
        }
        String contentType = normalizeContentType(file.getContentType());
        if (!EXTENSIONS_BY_CONTENT_TYPE.containsKey(contentType)) {
            throw new InvalidRequestException("INVALID_IMAGE_TYPE", "지원하지 않는 이미지 형식입니다.");
        }
        if (productImageRepository.countByProductId(productId) >= storageProperties.productImage().maxImagesPerProduct()) {
            throw new InvalidRequestException("TOO_MANY_PRODUCT_IMAGES", "상품 이미지 개수 제한을 초과했습니다.");
        }
    }

    private String createObjectKey(Long productId, String contentType) {
        String extension = EXTENSIONS_BY_CONTENT_TYPE.get(contentType);
        return "products/" + productId + "/" + UUID.randomUUID() + "." + extension;
    }

    private Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다."));
    }

    private ProductImage getOwnedImage(Long productId, Long imageId) {
        return productImageRepository.findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("IMAGE_NOT_FOUND", "상품 이미지를 찾을 수 없습니다."));
    }

    private void demoteCurrentMain(Long productId) {
        productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId).stream()
                .filter(ProductImage::isMain)
                .forEach(ProductImage::markDetail);
    }

    private void promoteNextMain(Long productId) {
        productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId).stream()
                .min(Comparator.comparingInt(ProductImage::getDisplayOrder).thenComparing(ProductImage::getId))
                .ifPresent(ProductImage::markMain);
    }

    private int nextDisplayOrder(Long productId) {
        return productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId).stream()
                .mapToInt(ProductImage::getDisplayOrder)
                .max()
                .orElse(-1) + 1;
    }

    private String resolveImageUrl(ProductImage image) {
        if (image.getObjectKey() == null || image.getObjectKey().isBlank()) {
            return image.getImageUrl();
        }
        return objectStorageService.publicUrl(image.getObjectKey());
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.toLowerCase();
    }

    private String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return null;
        }
        String filename = originalFilename.replace("\\", "/");
        filename = filename.substring(filename.lastIndexOf('/') + 1).trim();
        return filename.length() > 255 ? filename.substring(filename.length() - 255) : filename;
    }

    private void cleanupUploadedObject(String objectKey) {
        try {
            objectStorageService.delete(objectKey);
        } catch (RuntimeException cleanupException) {
            log.warn("Failed to clean up uploaded object after DB failure. objectKey={}", objectKey);
        }
    }

    private void scheduleStorageDeleteAfterCommit(Long productId, Long imageId, String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    objectStorageService.delete(objectKey);
                } catch (RuntimeException exception) {
                    log.warn("Product image metadata was deleted but object deletion failed. productId={} imageId={} objectKey={}",
                            productId, imageId, objectKey);
                }
            }
        });
    }
}
