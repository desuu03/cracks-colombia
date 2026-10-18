package co.uniquindio.crackscolombia.cracks_colombia_api.search.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record AthleteSearchCandidate(
        UUID athleteId,
        UUID photoStorageKey,
        String fullName,
        String sport,
        String position,
        LocalDate birthDate,
        String region
) {
}
