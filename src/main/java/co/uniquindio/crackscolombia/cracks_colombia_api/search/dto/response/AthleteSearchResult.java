package co.uniquindio.crackscolombia.cracks_colombia_api.search.dto.response;

import java.util.UUID;

public record AthleteSearchResult(
        UUID athleteId,
        String photoUrl,
        String fullName,
        String sport,
        String position,
        int age,
        String region
) {
}
