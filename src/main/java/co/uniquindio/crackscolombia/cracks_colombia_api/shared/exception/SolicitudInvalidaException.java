package co.uniquindio.crackscolombia.cracks_colombia_api.shared.exception;

import org.springframework.http.HttpStatus;

public class SolicitudInvalidaException extends ApiException {

    public SolicitudInvalidaException(String message) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_REQUEST", message);
    }
}
