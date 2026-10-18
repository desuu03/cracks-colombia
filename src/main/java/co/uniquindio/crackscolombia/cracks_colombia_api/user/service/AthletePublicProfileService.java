package co.uniquindio.crackscolombia.cracks_colombia_api.user.service;

import co.uniquindio.crackscolombia.cracks_colombia_api.match.entity.MatchRequestStatus;
import co.uniquindio.crackscolombia.cracks_colombia_api.match.repository.MatchRequestRepository;
import co.uniquindio.crackscolombia.cracks_colombia_api.match.repository.ScoutInterestRepository;
import co.uniquindio.crackscolombia.cracks_colombia_api.notification.service.NotificationService;
import co.uniquindio.crackscolombia.cracks_colombia_api.search.entity.AthleteProfile;
import co.uniquindio.crackscolombia.cracks_colombia_api.search.entity.AthleteProfileStatus;
import co.uniquindio.crackscolombia.cracks_colombia_api.shared.exception.ApiException;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.dto.response.AthletePublicProfileResponse;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.AthleteProfileDetails;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserAccount;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserRole;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.repository.AthleteProfileDetailsRepository;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.repository.UserAccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class AthletePublicProfileService {

    private final UserAccountRepository userAccountRepository;
    private final AthleteProfileDetailsRepository detailsRepository;
    private final ScoutInterestRepository scoutInterestRepository;
    private final MatchRequestRepository matchRequestRepository;
    private final NotificationService notificationService;
    private final ProfileDirectory profileDirectory;

    public AthletePublicProfileService(UserAccountRepository userAccountRepository,
                                       AthleteProfileDetailsRepository detailsRepository,
                                       ScoutInterestRepository scoutInterestRepository,
                                       MatchRequestRepository matchRequestRepository,
                                       NotificationService notificationService,
                                       ProfileDirectory profileDirectory) {
        this.userAccountRepository = userAccountRepository;
        this.detailsRepository = detailsRepository;
        this.scoutInterestRepository = scoutInterestRepository;
        this.matchRequestRepository = matchRequestRepository;
        this.notificationService = notificationService;
        this.profileDirectory = profileDirectory;
    }

    /** Public profile; a scout's visit also notifies the athlete (once a day per scout). */
    @Transactional
    public AthletePublicProfileResponse get(UUID athleteId, Authentication authentication) {
        UserAccount athlete = userAccountRepository.findById(athleteId)
                .filter(account -> account.getRole() == UserRole.DEPORTISTA)
                .orElseThrow(() -> ApiException.notFound("No se encontró el perfil del deportista."));
        Optional<AthleteProfile> profile = profileDirectory.athleteProfile(athleteId);
        Optional<AthleteProfileDetails> details = detailsRepository.findByAthlete_Id(athleteId);

        UserAccount scout = viewingScout(authentication);
        boolean interested = false;
        boolean pendingRequest = false;
        if (scout != null) {
            interested = scoutInterestRepository.existsByScout_IdAndAthlete_Id(scout.getId(), athleteId);
            pendingRequest = matchRequestRepository.existsByScout_IdAndAthlete_IdAndStatus(
                    scout.getId(), athleteId, MatchRequestStatus.PENDIENTE);
            notificationService.recordProfileView(scout, athlete, profileDirectory.scoutDisplayName(scout));
        }

        boolean photoIsPublic = profile.filter(p -> p.getStatus() == AthleteProfileStatus.APROBADO)
                .map(AthleteProfile::getPhotoStorageKey).isPresent();
        return new AthletePublicProfileResponse(
                athlete.getId(), athlete.getFullName(),
                profile.map(AthleteProfile::getSport).orElse(null),
                profile.map(AthleteProfile::getPosition).orElse(null),
                profile.map(AthleteProfile::getRegion).orElse(null),
                profileDirectory.ageOf(athlete),
                details.map(AthleteProfileDetails::getHeightCm).orElse(null),
                details.map(AthleteProfileDetails::getWeightKg).orElse(null),
                details.map(AthleteProfileDetails::getTrajectory).orElse(null),
                photoIsPublic ? "/athletes/" + athleteId + "/photo" : null,
                interested,
                pendingRequest);
    }

    private UserAccount viewingScout(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return userAccountRepository.findByEmail(authentication.getName())
                .filter(account -> account.getRole() == UserRole.OJEADOR_ENTRENADOR)
                .orElse(null);
    }
}
