package studio.aroundhub.tunagiftset.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalObjectStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void uploadCreatesFileWithGeneratedObjectKey() throws Exception {
        LocalObjectStorageService storage = storage();

        StoredFile storedFile = storage.upload(
                "products/10/sample.jpg",
                new ByteArrayInputStream(new byte[] {1, 2, 3}),
                3,
                "image/jpeg"
        );

        assertThat(storedFile.objectKey()).isEqualTo("products/10/sample.jpg");
        assertThat(storedFile.publicUrl()).isEqualTo("http://localhost:8080/uploads/products/10/sample.jpg");
        assertThat(Files.readAllBytes(tempDir.resolve("products/10/sample.jpg"))).containsExactly(1, 2, 3);
    }

    @Test
    void deleteRemovesFile() throws Exception {
        LocalObjectStorageService storage = storage();
        storage.upload("products/10/sample.png", new ByteArrayInputStream(new byte[] {1}), 1, "image/png");

        storage.delete("products/10/sample.png");

        assertThat(tempDir.resolve("products/10/sample.png")).doesNotExist();
    }

    @Test
    void uploadRejectsPathTraversal() {
        LocalObjectStorageService storage = storage();

        assertThatThrownBy(() -> storage.upload("../evil.jpg", new ByteArrayInputStream(new byte[] {1}), 1, "image/jpeg"))
                .isInstanceOf(StorageException.class)
                .hasMessage("저장 경로가 올바르지 않습니다.");

        assertThat(tempDir.resolveSibling("evil.jpg")).doesNotExist();
    }

    private LocalObjectStorageService storage() {
        return new LocalObjectStorageService(new StorageProperties(
                StorageType.LOCAL,
                "http://localhost:8080/uploads",
                new StorageProperties.ProductImage(5L * 1024L * 1024L, 10),
                new StorageProperties.Local(tempDir),
                new StorageProperties.S3("", "ap-northeast-2", "", false)
        ));
    }
}
