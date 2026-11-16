package co.uniquindio.crackscolombia.cracks_colombia_api.shared.exception;

import org.springframework.http.HttpStatus;

public class CredencialesInvalidasException extends ApiException {

    public CredencialesInvalidasException() {
        super(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Correo o contraseña incorrectos.");
    }
}
