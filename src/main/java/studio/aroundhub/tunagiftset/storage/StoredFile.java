package studio.aroundhub.tunagiftset.storage;

public record StoredFile(
        String objectKey,
        String publicUrl,
        String contentType,
        long size
) {
}
