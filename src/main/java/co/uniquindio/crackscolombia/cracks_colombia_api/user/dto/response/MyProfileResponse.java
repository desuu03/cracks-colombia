package co.uniquindio.crackscolombia.cracks_colombia_api.user.dto.response;

import co.uniquindio.crackscolombia.cracks_colombia_api.search.entity.AthleteProfileStatus;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserRole;

import java.time.LocalDate;
import java.util.UUID;

/** UserProfile of the OpenAPI contract: the authenticated user's own profile. */
public record MyProfileResponse(
        UUID id,
        String email,
        String fullName,
        UserRole role,
        LocalDate birthDate,
        String sport,
        String position,
        String region,
        Integer heightCm,
        Integer weightKg,
        String trajectory,
        AthleteProfileStatus profileStatus,
        String club,
        String contactPhone
) {
}
