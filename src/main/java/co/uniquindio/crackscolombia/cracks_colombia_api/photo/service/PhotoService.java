package co.uniquindio.crackscolombia.cracks_colombia_api.photo.service;

import co.uniquindio.crackscolombia.cracks_colombia_api.photo.dto.response.AthletePhotoResponse;
import co.uniquindio.crackscolombia.cracks_colombia_api.search.entity.AthleteProfile;
import co.uniquindio.crackscolombia.cracks_colombia_api.search.entity.AthleteProfileStatus;
import co.uniquindio.crackscolombia.cracks_colombia_api.search.repository.AthleteProfileRepository;
import co.uniquindio.crackscolombia.cracks_colombia_api.shared.exception.ApiException;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserAccount;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserRole;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.repository.UserAccountRepository;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.service.AthleteConsentPolicy;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class PhotoService {

    private final AthleteProfileRepository athleteProfileRepository;
    private final UserAccountRepository userAccountRepository;
    private final PhotoStorageService photoStorageService;
    private final AthleteConsentPolicy athleteConsentPolicy;

    public PhotoService(AthleteProfileRepository athleteProfileRepository,
                        UserAccountRepository userAccountRepository,
                        PhotoStorageService photoStorageService,
                        AthleteConsentPolicy athleteConsentPolicy) {
        this.athleteProfileRepository = athleteProfileRepository;
        this.userAccountRepository = userAccountRepository;
        this.photoStorageService = photoStorageService;
        this.athleteConsentPolicy = athleteConsentPolicy;
    }

    @Transactional
    public AthletePhotoResponse upload(UUID athleteId, String authenticatedEmail, MultipartFile upload) {
        UserAccount owner = requireAthlete(authenticatedEmail);
        if (!owner.getId().equals(athleteId)) {
            throw ApiException.forbidden("Solo puedes cambiar la foto de tu propio perfil.");
        }
        athleteConsentPolicy.requireMediaUploadConsent(owner, "fotos");

        PhotoStorageService.StoredPhoto stored = photoStorageService.store(upload);
        cleanupStoredFileAfterRollback(stored.storageKey());

        AthleteProfile profile = athleteProfileRepository.findByAthlete_Id(owner.getId())
                .orElseGet(() -> new AthleteProfile(owner, null, null, null, AthleteProfileStatus.PENDIENTE));
        UUID previousStorageKey = profile.getPhotoStorageKey();
        profile.setPhoto(stored.storageKey(), stored.contentType(), stored.sizeBytes());
        athleteProfileRepository.saveAndFlush(profile);
        if (previousStorageKey != null) {
            deleteStoredFileAfterCommit(previousStorageKey);
        }

        return new AthletePhotoResponse(owner.getId(), photoUrl(owner.getId()),
                stored.contentType(), stored.sizeBytes());
    }

    @Transactional(readOnly = true)
    public PhotoContent loadContent(UUID athleteId, Authentication authentication) {
        UserAccount athlete = userAccountRepository.findById(athleteId)
                .filter(account -> account.getRole() == UserRole.DEPORTISTA)
                .orElseThrow(() -> ApiException.notFound("No se encontró el perfil del deportista."));
        AthleteProfile profile = athleteProfileRepository.findByAthlete_Id(athleteId)
                .orElseThrow(() -> ApiException.notFound("No se encontró la foto del deportista."));
        if (profile.getPhotoStorageKey() == null || profile.getPhotoContentType() == null) {
            throw ApiException.notFound("No se encontró la foto del deportista.");
        }

        if (!mayViewPhoto(profile, athleteId, authentication)) {
            throw ApiException.notFound("No se encontró la foto del deportista.");
        }

        return new PhotoContent(photoStorageService.load(profile.getPhotoStorageKey()),
                profile.getPhotoContentType());
    }

    private boolean mayViewPhoto(AthleteProfile profile, UUID athleteId, Authentication authentication) {
        if (profile.getStatus() == AthleteProfileStatus.APROBADO) {
            return true;
        }
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        UserAccount requester = userAccountRepository.findByEmail(authentication.getName()).orElse(null);
        return requester != null && (requester.getId().equals(athleteId)
                || requester.getRole() == UserRole.MODERADOR);
    }

    private UserAccount requireAthlete(String email) {
        UserAccount account = userAccountRepository.findByEmail(email)
                .orElseThrow(ApiException::unauthenticated);
        if (account.getRole() != UserRole.DEPORTISTA) {
            throw ApiException.forbidden("Esta operación solo está disponible para deportistas.");
        }
        return account;
    }

    private String photoUrl(UUID athleteId) {
        return "/athletes/" + athleteId + "/photo";
    }

    private void cleanupStoredFileAfterRollback(UUID storageKey) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    photoStorageService.deleteQuietly(storageKey);
                }
            }
        });
    }

    private void deleteStoredFileAfterCommit(UUID storageKey) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                photoStorageService.deleteQuietly(storageKey);
            }
        });
    }

    public record PhotoContent(Resource resource, String contentType) {
    }
}
