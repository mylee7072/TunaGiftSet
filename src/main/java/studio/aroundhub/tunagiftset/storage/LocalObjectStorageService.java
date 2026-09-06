package studio.aroundhub.tunagiftset.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "storage", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalObjectStorageService implements ObjectStorageService {

    private final Path rootDirectory;
    private final StorageProperties properties;

    public LocalObjectStorageService(StorageProperties properties) {
        this.rootDirectory = properties.local().uploadDir().toAbsolutePath().normalize();
        this.properties = properties;
    }

    @Override
    public StoredFile upload(String objectKey, InputStream inputStream, long contentLength, String contentType) {
        Path target = resolveSafePath(objectKey);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredFile(objectKey, publicUrl(objectKey), contentType, contentLength);
        } catch (IOException exception) {
            throw new StorageException("IMAGE_UPLOAD_FAILED", "이미지 업로드 중 오류가 발생했습니다.", exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        Path target = resolveSafePath(objectKey);
        try {
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            throw new StorageException("IMAGE_DELETE_FAILED", "이미지 삭제 중 오류가 발생했습니다.", exception);
        }
    }

    @Override
    public String publicUrl(String objectKey) {
        return properties.publicBaseUrl() + "/" + objectKey;
    }

    private Path resolveSafePath(String objectKey) {
        Path target = rootDirectory.resolve(objectKey).normalize();
        if (!target.startsWith(rootDirectory)) {
            throw new StorageException("INVALID_OBJECT_KEY", "저장 경로가 올바르지 않습니다.");
        }
        return target;
    }
}
