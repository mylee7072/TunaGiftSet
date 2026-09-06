package studio.aroundhub.tunagiftset.storage;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage")
public record StorageProperties(
        StorageType type,
        String publicBaseUrl,
        ProductImage productImage,
        Local local,
        S3 s3
) {
    public StorageType type() {
        return type == null ? StorageType.LOCAL : type;
    }

    public String publicBaseUrl() {
        return trimTrailingSlash(publicBaseUrl == null || publicBaseUrl.isBlank() ? "/uploads" : publicBaseUrl);
    }

    public ProductImage productImage() {
        return productImage == null ? new ProductImage(5L * 1024L * 1024L, 10) : productImage;
    }

    public Local local() {
        return local == null ? new Local(Path.of(System.getProperty("user.home"), "tunagiftset", "uploads")) : local;
    }

    public S3 s3() {
        return s3 == null ? new S3("", "ap-northeast-2", "", false) : s3;
    }

    private String trimTrailingSlash(String value) {
        String result = value.trim();
        while (result.endsWith("/") && result.length() > 1) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    public record ProductImage(
            long maxFileSizeBytes,
            int maxImagesPerProduct
    ) {
        public ProductImage {
            if (maxFileSizeBytes <= 0) {
                maxFileSizeBytes = 5L * 1024L * 1024L;
            }
            if (maxImagesPerProduct <= 0) {
                maxImagesPerProduct = 10;
            }
        }
    }

    public record Local(Path uploadDir) {
        public Local {
            if (uploadDir == null) {
                uploadDir = Path.of(System.getProperty("user.home"), "tunagiftset", "uploads");
            }
        }
    }

    public record S3(
            String bucket,
            String region,
            String endpoint,
            boolean pathStyleAccess
    ) {
        public S3 {
            if (region == null || region.isBlank()) {
                region = "ap-northeast-2";
            }
        }
    }
}
