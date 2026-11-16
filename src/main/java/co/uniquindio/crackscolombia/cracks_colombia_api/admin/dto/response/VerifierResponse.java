package co.uniquindio.crackscolombia.cracks_colombia_api.admin.dto.response;

import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserRole;

import java.time.Instant;
import java.util.UUID;

public record VerifierResponse(
        UUID id,
        String email,
        String fullName,
        UserRole role,
        boolean active,
        Instant createdAt
) {
}
