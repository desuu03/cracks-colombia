package co.uniquindio.crackscolombia.cracks_colombia_api.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "El correo es obligatorio.")
        @Size(max = 254, message = "El correo no puede superar 254 caracteres.")
        String email,

        @NotBlank(message = "La contraseña es obligatoria.")
        String password
) {
}
