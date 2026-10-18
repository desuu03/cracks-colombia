package co.uniquindio.crackscolombia.cracks_colombia_api.notification.service;

import co.uniquindio.crackscolombia.cracks_colombia_api.notification.dto.response.NotificationResponse;
import co.uniquindio.crackscolombia.cracks_colombia_api.notification.entity.Notification;
import co.uniquindio.crackscolombia.cracks_colombia_api.notification.entity.NotificationType;
import co.uniquindio.crackscolombia.cracks_colombia_api.notification.entity.ProfileView;
import co.uniquindio.crackscolombia.cracks_colombia_api.notification.repository.NotificationRepository;
import co.uniquindio.crackscolombia.cracks_colombia_api.notification.repository.ProfileViewRepository;
import co.uniquindio.crackscolombia.cracks_colombia_api.shared.exception.ApiException;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserAccount;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ProfileViewRepository profileViewRepository;
    private final UserAccountRepository userAccountRepository;
    private final Clock clock;

    public NotificationService(NotificationRepository notificationRepository,
                               ProfileViewRepository profileViewRepository,
                               UserAccountRepository userAccountRepository,
                               Clock clock) {
        this.notificationRepository = notificationRepository;
        this.profileViewRepository = profileViewRepository;
        this.userAccountRepository = userAccountRepository;
        this.clock = clock;
    }

    /** Creates a notification inside the caller's transaction. */
    @Transactional
    public void notify(UserAccount recipient, NotificationType type, String message) {
        notificationRepository.save(new Notification(recipient, type, message, clock.instant()));
    }

    /** Records that a scout opened an athlete's profile; notifies the athlete at most once a day per scout. */
    @Transactional
    public void recordProfileView(UserAccount scout, UserAccount athlete, String scoutDisplayName) {
        LocalDate today = LocalDate.now(clock);
        if (profileViewRepository.existsByScout_IdAndAthlete_IdAndViewedOn(scout.getId(), athlete.getId(), today)) {
            return;
        }
        profileViewRepository.save(new ProfileView(scout, athlete, today));
        notify(athlete, NotificationType.PROFILE_VIEWED, scoutDisplayName + " vio tu perfil.");
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listOwn(String authenticatedEmail, boolean unreadOnly) {
        UUID userId = requireAccount(authenticatedEmail).getId();
        List<Notification> notifications = unreadOnly
                ? notificationRepository.findTop50ByRecipient_IdAndReadFalseOrderByCreatedAtDesc(userId)
                : notificationRepository.findTop50ByRecipient_IdOrderByCreatedAtDesc(userId);
        return notifications.stream().map(this::toResponse).toList();
    }

    @Transactional
    public void markRead(UUID notificationId, String authenticatedEmail) {
        UUID userId = requireAccount(authenticatedEmail).getId();
        Notification notification = notificationRepository.findByIdAndRecipient_Id(notificationId, userId)
                .orElseThrow(() -> ApiException.notFound("No se encontró la notificación."));
        notification.markRead();
    }

    private UserAccount requireAccount(String email) {
        return userAccountRepository.findByEmail(email).orElseThrow(ApiException::unauthenticated);
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getType(), notification.getMessage(),
                notification.isRead(), notification.getCreatedAt());
    }
}
