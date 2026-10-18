package co.uniquindio.crackscolombia.cracks_colombia_api.match.dto.response;

import co.uniquindio.crackscolombia.cracks_colombia_api.match.entity.MatchRequestStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * MatchRequest of the OpenAPI contract plus the names the inbox shows. contact
 * holds the other party's details and is only present once the request is accepted.
 */
public record MatchRequestResponse(
        UUID id,
        UUID athleteId,
        UUID scoutId,
        String message,
        Instant proposedDate,
        MatchRequestStatus status,
        Instant contactSharedAt,
        Instant createdAt,
        AthleteSummary athlete,
        ScoutSummary scout,
        Contact contact
) {
    public record AthleteSummary(UUID id, String fullName, String sport, String position, String region) {
    }

    public record ScoutSummary(UUID id, String fullName, String club) {
    }

    public record Contact(String phone, String email) {
    }
}
