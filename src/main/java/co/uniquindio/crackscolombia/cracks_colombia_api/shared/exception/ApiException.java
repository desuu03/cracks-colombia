package co.uniquindio.crackscolombia.cracks_colombia_api.shared.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public static ApiException invalid(String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_REQUEST", message);
    }

    public static ApiException invalidVideo(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_VIDEO", message);
    }

    public static ApiException invalidPhoto(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PHOTO", message);
    }

    public static ApiException videoTooLarge(long maxFileSizeBytes) {
        return new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "VIDEO_TOO_LARGE",
                "El video supera el limite permitido de " + (maxFileSizeBytes / 1_000_000) + " MB.");
    }

    public static ApiException photoTooLarge(long maxFileSizeBytes) {
        return new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "PHOTO_TOO_LARGE",
                "La foto supera el limite permitido de " + (maxFileSizeBytes / 1_000_000) + " MB.");
    }

    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public static ApiException unauthenticated() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                "La cuenta asociada a la sesion ya no esta disponible.");
    }

    public static ApiException emailAlreadyRegistered() {
        return new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED",
                "Este correo ya está registrado.");
    }

    public static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                "Correo o contraseña incorrectos.");
    }
}
