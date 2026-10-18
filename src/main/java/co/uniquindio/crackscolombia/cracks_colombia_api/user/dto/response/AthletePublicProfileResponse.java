package co.uniquindio.crackscolombia.cracks_colombia_api.user.dto.response;

import java.util.UUID;

/**
 * Public view of an athlete. Carries the age instead of the birth date and no
 * email, because the page is public and many athletes are minors.
 * interestedByMe and pendingRequestByMe only apply to a scout's token.
 */
public record AthletePublicProfileResponse(
        UUID id,
        String fullName,
        String sport,
        String position,
        String region,
        Integer age,
        Integer heightCm,
        Integer weightKg,
        String trajectory,
        String photoUrl,
        boolean interestedByMe,
        boolean pendingRequestByMe
) {
}
