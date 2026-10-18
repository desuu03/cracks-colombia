package co.uniquindio.crackscolombia.cracks_colombia_api.video.service;

import co.uniquindio.crackscolombia.cracks_colombia_api.shared.exception.ApiException;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserAccount;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserRole;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.repository.UserAccountRepository;
import co.uniquindio.crackscolombia.cracks_colombia_api.video.dto.response.VideoResponse;
import co.uniquindio.crackscolombia.cracks_colombia_api.video.entity.Video;
import co.uniquindio.crackscolombia.cracks_colombia_api.video.entity.VideoStatus;
import co.uniquindio.crackscolombia.cracks_colombia_api.video.repository.VideoRepository;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.Period;
import java.util.List;
import java.util.UUID;

@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final UserAccountRepository userAccountRepository;
    private final VideoStorageService videoStorageService;
    private final Clock clock;

    public VideoService(VideoRepository videoRepository,
                        UserAccountRepository userAccountRepository,
                        VideoStorageService videoStorageService,
                        Clock clock) {
        this.videoRepository = videoRepository;
        this.userAccountRepository = userAccountRepository;
        this.videoStorageService = videoStorageService;
        this.clock = clock;
    }

    @Transactional
    public VideoResponse create(UUID athleteId, String authenticatedEmail, MultipartFile upload) {
        UserAccount owner = requireAthlete(authenticatedEmail);
        if (!owner.getId().equals(athleteId)) {
            throw ApiException.forbidden("Solo puedes agregar videos a tu propio perfil.");
        }
        requireVideoConsent(owner);

        VideoStorageService.StoredVideo stored = videoStorageService.store(upload);
        cleanupStoredFileAfterRollback(stored.storageKey());
        Video video = new Video(owner, stored.storageKey(), stored.sizeBytes(),
                stored.durationSeconds(), clock.instant());
        return toResponse(videoRepository.saveAndFlush(video));
    }

    @Transactional(readOnly = true)
    public List<VideoResponse> listOwn(String authenticatedEmail) {
        UserAccount athlete = requireAthlete(authenticatedEmail);
        return videoRepository.findAllByAthlete_IdOrderByUploadedAtDesc(athlete.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<VideoResponse> listApproved(UUID athleteId) {
        UserAccount athlete = userAccountRepository.findById(athleteId)
                .filter(account -> account.getRole() == UserRole.DEPORTISTA)
                .orElseThrow(() -> ApiException.notFound("No se encontro el perfil del deportista."));
        return videoRepository.findAllByAthlete_IdAndStatusOrderByUploadedAtDesc(
                        athlete.getId(), VideoStatus.APROBADO)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public void delete(UUID videoId, String authenticatedEmail) {
        UserAccount athlete = requireAthlete(authenticatedEmail);
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> ApiException.notFound("No se encontro el video."));
        if (!video.getAthlete().getId().equals(athlete.getId())) {
            throw ApiException.forbidden("Solo puedes eliminar tus propios videos.");
        }
        UUID storageKey = video.getStorageKey();
        videoRepository.delete(video);
        deleteStoredFileAfterCommit(storageKey);
    }

    @Transactional(readOnly = true)
    public Resource loadContent(UUID videoId, Authentication authentication) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> ApiException.notFound("No se encontro el video."));
        boolean mayView = video.getStatus() == VideoStatus.APROBADO;
        if (authentication != null && authentication.isAuthenticated()) {
            UserAccount requester = userAccountRepository.findByEmail(authentication.getName()).orElse(null);
            if (requester != null) {
                mayView = mayView || requester.getId().equals(video.getAthlete().getId())
                        || requester.getRole() == UserRole.MODERADOR;
            }
        }
        if (!mayView) {
            // Hide pending and rejected content from the public and scouting roles.
            throw ApiException.notFound("No se encontro el video.");
        }
        return videoStorageService.load(video.getStorageKey());
    }

    private UserAccount requireAthlete(String email) {
        UserAccount account = userAccountRepository.findByEmail(email)
                .orElseThrow(ApiException::unauthenticated);
        if (account.getRole() != UserRole.DEPORTISTA) {
            throw ApiException.forbidden("Esta operacion solo esta disponible para deportistas.");
        }
        return account;
    }

    private void requireVideoConsent(UserAccount account) {
        if (account.getBirthDate() == null) {
            throw ApiException.invalid("El perfil del deportista debe tener fecha de nacimiento.");
        }
        if (!account.isDataProcessingConsentAccepted()) {
            throw ApiException.forbidden("Debes aceptar la politica de tratamiento de datos antes de cargar videos.");
        }
        boolean minor = Period.between(account.getBirthDate(), java.time.LocalDate.now(clock)).getYears() < 18;
        if (minor && (!account.isGuardianConsentAccepted()
                || account.getGuardianConsentAcceptedAt() == null)) {
            throw ApiException.forbidden(
                    "El deportista menor debe tener autorizacion de acudiente y tratamiento de datos aceptado.");
        }
    }

    private VideoResponse toResponse(Video video) {
        return new VideoResponse(video.getId(), video.getAthlete().getId(),
                "/videos/" + video.getId() + "/content", video.getSizeBytes(),
                video.getDurationSeconds(), video.getStatus(), video.getRejectionReason(), video.getUploadedAt());
    }

    private void cleanupStoredFileAfterRollback(UUID storageKey) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    videoStorageService.deleteQuietly(storageKey);
                }
            }
        });
    }

    private void deleteStoredFileAfterCommit(UUID storageKey) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                videoStorageService.deleteQuietly(storageKey);
            }
        });
    }
}
