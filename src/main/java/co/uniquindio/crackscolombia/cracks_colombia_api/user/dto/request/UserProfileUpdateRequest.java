package co.uniquindio.crackscolombia.cracks_colombia_api.user.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of PUT /users/me. Athletes send the sport fields, scouts send club and
 * phone; fields for the other role are ignored. birthDate is deliberately absent:
 * it decides whether guardian consent is required, so it cannot change here.
 */
public record UserProfileUpdateRequest(
        String sport,
        String position,
        String region,
        @Min(value = 100, message = "La estatura debe estar entre 100 y 230 cm.")
        @Max(value = 230, message = "La estatura debe estar entre 100 y 230 cm.")
        Integer heightCm,
        @Min(value = 30, message = "El peso debe estar entre 30 y 150 kg.")
        @Max(value = 150, message = "El peso debe estar entre 30 y 150 kg.")
        Integer weightKg,
        @Size(max = 1000, message = "La trayectoria no puede superar 1.000 caracteres.")
        String trajectory,
        @Size(max = 120, message = "El club no puede superar 120 caracteres.")
        String club,
        @Size(max = 30, message = "El teléfono no puede superar 30 caracteres.")
        @Pattern(regexp = "^[+0-9 ()-]*$", message = "El teléfono solo puede tener números, espacios y los signos + ( ) -.")
        String contactPhone
) {
}
