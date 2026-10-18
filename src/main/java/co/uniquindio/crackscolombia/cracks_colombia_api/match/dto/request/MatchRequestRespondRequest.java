package co.uniquindio.crackscolombia.cracks_colombia_api.match.dto.request;

import jakarta.validation.constraints.NotNull;

/** The contract takes ACCEPTED/REJECTED; the resulting status is ACEPTADA/RECHAZADA. */
public record MatchRequestRespondRequest(
        @NotNull(message = "Debes indicar si aceptas o rechazas la solicitud.")
        Decision decision
) {
    public enum Decision {
        ACCEPTED,
        REJECTED
    }
}
