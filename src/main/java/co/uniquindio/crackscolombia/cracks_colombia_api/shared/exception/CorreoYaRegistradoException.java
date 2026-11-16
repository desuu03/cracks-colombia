package co.uniquindio.crackscolombia.cracks_colombia_api.shared.exception;

import org.springframework.http.HttpStatus;

public class CorreoYaRegistradoException extends ApiException {

    public CorreoYaRegistradoException() {
        super(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "Este correo ya está registrado.");
    }
}
