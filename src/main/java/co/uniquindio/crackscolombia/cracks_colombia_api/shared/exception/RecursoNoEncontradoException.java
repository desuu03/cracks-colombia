package co.uniquindio.crackscolombia.cracks_colombia_api.shared.exception;

import org.springframework.http.HttpStatus;

public class RecursoNoEncontradoException extends ApiException {

    public RecursoNoEncontradoException(String message) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }
}
