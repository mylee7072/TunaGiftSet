package studio.aroundhub.tunagiftset.storage;

import java.io.IOException;
import java.io.InputStream;

public interface ObjectStorageService {

    StoredFile upload(String objectKey, InputStream inputStream, long contentLength, String contentType) throws IOException;

    void delete(String objectKey);

    String publicUrl(String objectKey);
}
