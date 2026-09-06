package studio.aroundhub.tunagiftset.exception;

import java.time.Instant;

public record ErrorResponse(
        int status,
        String code,
        String message,
        String path,
        Instant timestamp
) {
    public static ErrorResponse of(int status, String code, String message, String path) {
        return new ErrorResponse(status, code, message, path, Instant.now());
    }
}
