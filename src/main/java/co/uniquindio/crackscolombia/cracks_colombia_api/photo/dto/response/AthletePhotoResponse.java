package co.uniquindio.crackscolombia.cracks_colombia_api.photo.dto.response;

import java.util.UUID;

public record AthletePhotoResponse(UUID athleteId, String photoUrl, String contentType, long sizeBytes) {
}
