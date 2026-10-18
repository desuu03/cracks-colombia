package co.uniquindio.crackscolombia.cracks_colombia_api.auth.dto.request;

import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserRole;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo no tiene un formato válido.")
        @Size(max = 254, message = "El correo no puede superar 254 caracteres.")
        String email,

        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres.")
        String password,

        @NotNull(message = "Debes seleccionar un rol.")
        UserRole role,

        @NotBlank(message = "El nombre completo es obligatorio.")
        @Size(max = 150, message = "El nombre completo no puede superar 150 caracteres.")
        String fullName,

        LocalDate birthDate,

        GuardianConsentRequest guardianConsent,

        @NotNull(message = "Debes aceptar la política de tratamiento de datos.")
        @AssertTrue(message = "Debes aceptar la política de tratamiento de datos.")
        Boolean dataProcessingConsent
) {
}
