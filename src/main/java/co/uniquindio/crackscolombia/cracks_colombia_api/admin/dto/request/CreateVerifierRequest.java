package co.uniquindio.crackscolombia.cracks_colombia_api.admin.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateVerifierRequest(
        @NotBlank(message = "El nombre completo es obligatorio.")
        @Size(max = 150, message = "El nombre completo no puede superar 150 caracteres.")
        String fullName,

        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo no tiene un formato válido.")
        @Size(max = 254, message = "El correo no puede superar 254 caracteres.")
        String email,

        @NotBlank(message = "La contraseña temporal es obligatoria.")
        @Size(min = 8, message = "La contraseña temporal debe tener al menos 8 caracteres.")
        String temporaryPassword
) {
}
