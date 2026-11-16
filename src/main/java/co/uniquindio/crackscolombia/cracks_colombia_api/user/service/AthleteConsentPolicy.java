package co.uniquindio.crackscolombia.cracks_colombia_api.user.service;

import co.uniquindio.crackscolombia.cracks_colombia_api.shared.exception.ApiException;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserAccount;
import org.springframework.stereotype.Component;

@Component
public class AthleteConsentPolicy {

    private final ProfileDirectory profileDirectory;

    public AthleteConsentPolicy(ProfileDirectory profileDirectory) {
        this.profileDirectory = profileDirectory;
    }

    public void requireMediaUploadConsent(UserAccount account, String mediaLabel) {
        if (account.getBirthDate() == null) {
            throw ApiException.invalid("El perfil del deportista debe tener fecha de nacimiento.");
        }
        if (!account.isDataProcessingConsentAccepted()) {
            throw ApiException.forbidden(
                    "Debes aceptar la política de tratamiento de datos antes de cargar " + mediaLabel + ".");
        }
        if (profileDirectory.isMinor(account) && (!account.isGuardianConsentAccepted()
                || account.getGuardianConsentAcceptedAt() == null)) {
            throw ApiException.forbidden(
                    "El deportista menor debe tener autorización de acudiente y tratamiento de datos aceptado.");
        }
    }
}
