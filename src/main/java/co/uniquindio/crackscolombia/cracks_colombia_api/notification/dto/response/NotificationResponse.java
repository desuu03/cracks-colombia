package co.uniquindio.crackscolombia.cracks_colombia_api.notification.dto.response;

import co.uniquindio.crackscolombia.cracks_colombia_api.notification.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String message,
        boolean read,
        Instant createdAt
) {
}
