package co.uniquindio.crackscolombia.cracks_colombia_api.moderation.service;

import co.uniquindio.crackscolombia.cracks_colombia_api.moderation.dto.response.ModerationQueuePage;
import co.uniquindio.crackscolombia.cracks_colombia_api.moderation.dto.response.ModerationVideoResponse;
import co.uniquindio.crackscolombia.cracks_colombia_api.search.entity.AthleteProfile;
import co.uniquindio.crackscolombia.cracks_colombia_api.shared.exception.ApiException;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserAccount;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserRole;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.repository.UserAccountRepository;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.service.ProfileDirectory;
import co.uniquindio.crackscolombia.cracks_colombia_api.video.entity.Video;
import co.uniquindio.crackscolombia.cracks_colombia_api.video.entity.VideoStatus;
import co.uniquindio.crackscolombia.cracks_colombia_api.video.repository.VideoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class ModerationService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_REASON_LENGTH = 500;

    private final VideoRepository videoRepository;
    private final UserAccountRepository userAccountRepository;
    private final ProfileDirectory profileDirectory;
    private final Clock clock;

    public ModerationService(VideoRepository videoRepository,
                             UserAccountRepository userAccountRepository,
                             ProfileDirectory profileDirectory,
                             Clock clock) {
        this.videoRepository = videoRepository;
        this.userAccountRepository = userAccountRepository;
        this.profileDirectory = profileDirectory;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ModerationQueuePage queue(String authenticatedEmail, int page, int size) {
        requireModerator(authenticatedEmail);
        validatePage(page, size);
        Page<Video> results = videoRepository.findAllByStatusOrderByUploadedAtAsc(
                VideoStatus.PENDIENTE, PageRequest.of(page, size));
        return new ModerationQueuePage(
                results.getContent().stream().map(this::toResponse).toList(),
                results.getNumber(),
                results.getSize(),
                results.getTotalElements(),
                results.getTotalPages(),
                results.isFirst(),
                results.isLast());
    }

    @Transactional
    public ModerationVideoResponse approve(UUID videoId, String authenticatedEmail) {
        UserAccount moderator = requireModerator(authenticatedEmail);
        Video video = requirePending(requireVideo(videoId));
        video.markApproved(moderator, clock.instant());
        return toResponse(videoRepository.saveAndFlush(video));
    }

    @Transactional
    public ModerationVideoResponse reject(UUID videoId, String reason, String authenticatedEmail) {
        UserAccount moderator = requireModerator(authenticatedEmail);
        Video video = requirePending(requireVideo(videoId));
        video.markRejected(moderator, requireReason(reason), clock.instant());
        return toResponse(videoRepository.saveAndFlush(video));
    }

    private Video requireVideo(UUID videoId) {
        return videoRepository.findById(videoId)
                .orElseThrow(() -> ApiException.notFound("No se encontró el video."));
    }

    private Video requirePending(Video video) {
        if (video.getStatus() != VideoStatus.PENDIENTE) {
            throw ApiException.videoAlreadyModerated();
        }
        return video;
    }

    private String requireReason(String reason) {
        String trimmed = reason == null ? "" : reason.trim();
        if (trimmed.isEmpty()) {
            throw ApiException.invalid("El motivo del rechazo es obligatorio.");
        }
        if (trimmed.length() > MAX_REASON_LENGTH) {
            throw ApiException.invalid("El motivo del rechazo no puede superar 500 caracteres.");
        }
        return trimmed;
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw ApiException.invalid("La página debe ser mayor o igual a cero.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw ApiException.invalid("El tamaño de página debe estar entre 1 y " + MAX_PAGE_SIZE + ".");
        }
    }

    private UserAccount requireModerator(String email) {
        UserAccount account = userAccountRepository.findByEmail(email)
                .orElseThrow(ApiException::unauthenticated);
        if (account.getRole() != UserRole.MODERADOR) {
            throw ApiException.forbidden("Esta operación solo está disponible para el moderador.");
        }
        return account;
    }

    private ModerationVideoResponse toResponse(Video video) {
        UserAccount athlete = video.getAthlete();
        UserAccount moderator = video.getModeratedBy();
        String sport = profileDirectory.athleteProfile(athlete.getId())
                .map(AthleteProfile::getSport)
                .orElse(null);
        return new ModerationVideoResponse(
                video.getId(),
                athlete.getId(),
                athlete.getFullName(),
                profileDirectory.ageOf(athlete),
                profileDirectory.isMinor(athlete),
                sport,
                "/videos/" + video.getId() + "/content",
                video.getSizeBytes(),
                video.getDurationSeconds(),
                video.getStatus(),
                video.getRejectionReason(),
                video.getUploadedAt(),
                moderator == null ? null : moderator.getId(),
                moderator == null ? null : moderator.getFullName(),
                video.getModeratedAt());
    }
}
