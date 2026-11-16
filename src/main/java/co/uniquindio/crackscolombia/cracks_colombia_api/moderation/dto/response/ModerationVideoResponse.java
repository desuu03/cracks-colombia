package co.uniquindio.crackscolombia.cracks_colombia_api.moderation.dto.response;

import co.uniquindio.crackscolombia.cracks_colombia_api.video.entity.VideoStatus;

import java.time.Instant;
import java.util.UUID;

public record ModerationVideoResponse(
        UUID id,
        UUID athleteId,
        String athleteFullName,
        Integer athleteAge,
        boolean minor,
        String sport,
        String url,
        long sizeBytes,
        int durationSeconds,
        VideoStatus status,
        String rejectionReason,
        Instant uploadedAt,
        UUID moderatedById,
        String moderatedByName,
        Instant moderatedAt
) {
}
