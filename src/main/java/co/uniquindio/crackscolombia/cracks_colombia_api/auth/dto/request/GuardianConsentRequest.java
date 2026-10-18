package co.uniquindio.crackscolombia.cracks_colombia_api.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GuardianConsentRequest(
        @NotBlank(message = "El nombre del acudiente es obligatorio.")
        @Size(max = 150, message = "El nombre del acudiente no puede superar 150 caracteres.")
        String guardianFullName,

        @NotBlank(message = "El correo del acudiente es obligatorio.")
        @Email(message = "El correo del acudiente no tiene un formato válido.")
        @Size(max = 254, message = "El correo del acudiente no puede superar 254 caracteres.")
        String guardianEmail,

        @NotNull(message = "Debes indicar la autorización del acudiente.")
        Boolean accepted
) {
}
