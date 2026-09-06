package studio.aroundhub.tunagiftset.storage;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@ConditionalOnProperty(prefix = "storage", name = "type", havingValue = "s3")
public class S3ObjectStorageService implements ObjectStorageService {

    private final S3Client s3Client;
    private final StorageProperties properties;

    public S3ObjectStorageService(S3Client s3Client, StorageProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    @Override
    public StoredFile upload(String objectKey, InputStream inputStream, long contentLength, String contentType) throws IOException {
        if (properties.s3().bucket() == null || properties.s3().bucket().isBlank()) {
            throw new StorageException("STORAGE_CONFIGURATION_ERROR", "S3 bucket is not configured.");
        }

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(properties.s3().bucket())
                            .key(objectKey)
                            .contentType(contentType)
                            .contentLength(contentLength)
                            .build(),
                    RequestBody.fromInputStream(inputStream, contentLength)
            );
            return new StoredFile(objectKey, publicUrl(objectKey), contentType, contentLength);
        } catch (S3Exception exception) {
            throw new StorageException("IMAGE_UPLOAD_FAILED", "이미지 업로드 중 오류가 발생했습니다.", exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        if (properties.s3().bucket() == null || properties.s3().bucket().isBlank()) {
            throw new StorageException("STORAGE_CONFIGURATION_ERROR", "S3 bucket is not configured.");
        }

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.s3().bucket())
                    .key(objectKey)
                    .build());
        } catch (S3Exception exception) {
            throw new StorageException("IMAGE_DELETE_FAILED", "이미지 삭제 중 오류가 발생했습니다.", exception);
        }
    }

    @Override
    public String publicUrl(String objectKey) {
        return properties.publicBaseUrl() + "/" + objectKey;
    }
}
