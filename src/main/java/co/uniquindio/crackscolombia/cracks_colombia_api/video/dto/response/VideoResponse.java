package co.uniquindio.crackscolombia.cracks_colombia_api.video.dto.response;

import co.uniquindio.crackscolombia.cracks_colombia_api.video.entity.VideoStatus;

import java.time.Instant;
import java.util.UUID;

public record VideoResponse(
        UUID id,
        UUID athleteId,
        String url,
        long sizeBytes,
        int durationSeconds,
        VideoStatus status,
        String rejectionReason,
        Instant uploadedAt
) {
}
