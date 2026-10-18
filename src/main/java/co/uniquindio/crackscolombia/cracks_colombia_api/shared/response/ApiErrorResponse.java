package co.uniquindio.crackscolombia.cracks_colombia_api.shared.response;

import java.time.Instant;

public record ApiErrorResponse(String code, String message, Instant timestamp) {
}
