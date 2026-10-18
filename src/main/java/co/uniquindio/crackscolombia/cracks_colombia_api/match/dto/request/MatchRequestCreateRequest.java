package co.uniquindio.crackscolombia.cracks_colombia_api.match.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record MatchRequestCreateRequest(
        @NotNull(message = "Debes indicar el deportista.")
        UUID athleteId,
        @NotBlank(message = "Escribe un mensaje para el deportista.")
        @Size(max = 1000, message = "El mensaje no puede superar 1.000 caracteres.")
        String message,
        Instant proposedDate
) {
}
