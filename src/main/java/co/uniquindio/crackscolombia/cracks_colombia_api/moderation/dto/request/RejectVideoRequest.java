package co.uniquindio.crackscolombia.cracks_colombia_api.moderation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectVideoRequest(
        @NotBlank(message = "El motivo del rechazo es obligatorio.")
        @Size(max = 500, message = "El motivo del rechazo no puede superar 500 caracteres.")
        String reason
) {
}
