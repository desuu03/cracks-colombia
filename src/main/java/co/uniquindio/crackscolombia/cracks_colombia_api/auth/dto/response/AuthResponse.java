package co.uniquindio.crackscolombia.cracks_colombia_api.auth.dto.response;

public record AuthResponse(String accessToken, String tokenType, UserProfileResponse user) {
}
