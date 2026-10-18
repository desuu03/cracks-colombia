package co.uniquindio.crackscolombia.cracks_colombia_api.auth.dto.response;

import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserRole;

import java.time.LocalDate;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String fullName,
        UserRole role,
        LocalDate birthDate
) {
}
