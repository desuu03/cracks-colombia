package co.uniquindio.crackscolombia.cracks_colombia_api.match.service;

import co.uniquindio.crackscolombia.cracks_colombia_api.match.dto.request.MatchRequestCreateRequest;
import co.uniquindio.crackscolombia.cracks_colombia_api.match.dto.request.MatchRequestRespondRequest;
import co.uniquindio.crackscolombia.cracks_colombia_api.match.dto.response.MatchRequestResponse;
import co.uniquindio.crackscolombia.cracks_colombia_api.match.entity.MatchRequest;
import co.uniquindio.crackscolombia.cracks_colombia_api.match.entity.MatchRequestStatus;
import co.uniquindio.crackscolombia.cracks_colombia_api.match.entity.ScoutInterest;
import co.uniquindio.crackscolombia.cracks_colombia_api.match.repository.MatchRequestRepository;
import co.uniquindio.crackscolombia.cracks_colombia_api.match.repository.ScoutInterestRepository;
import co.uniquindio.crackscolombia.cracks_colombia_api.notification.entity.NotificationType;
import co.uniquindio.crackscolombia.cracks_colombia_api.notification.service.NotificationService;
import co.uniquindio.crackscolombia.cracks_colombia_api.search.entity.AthleteProfile;
import co.uniquindio.crackscolombia.cracks_colombia_api.shared.exception.ApiException;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.ScoutProfile;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserAccount;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserRole;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.repository.UserAccountRepository;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.service.ProfileDirectory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MatchRequestService {

    private final MatchRequestRepository matchRequestRepository;
    private final ScoutInterestRepository scoutInterestRepository;
    private final UserAccountRepository userAccountRepository;
    private final NotificationService notificationService;
    private final ProfileDirectory profileDirectory;
    private final Clock clock;

    public MatchRequestService(MatchRequestRepository matchRequestRepository,
                               ScoutInterestRepository scoutInterestRepository,
                               UserAccountRepository userAccountRepository,
                               NotificationService notificationService,
                               ProfileDirectory profileDirectory,
                               Clock clock) {
        this.matchRequestRepository = matchRequestRepository;
        this.scoutInterestRepository = scoutInterestRepository;
        this.userAccountRepository = userAccountRepository;
        this.notificationService = notificationService;
        this.profileDirectory = profileDirectory;
        this.clock = clock;
    }

    /** Idempotent: marking the same athlete twice keeps a single interest. */
    @Transactional
    public void markInterest(UUID athleteId, String authenticatedEmail) {
        UserAccount scout = requireRole(authenticatedEmail, UserRole.OJEADOR_ENTRENADOR);
        UserAccount athlete = requireAthlete(athleteId);
        if (!scoutInterestRepository.existsByScout_IdAndAthlete_Id(scout.getId(), athlete.getId())) {
            scoutInterestRepository.save(new ScoutInterest(scout, athlete, clock.instant()));
        }
    }

    @Transactional
    public MatchRequestResponse create(MatchRequestCreateRequest request, String authenticatedEmail) {
        UserAccount scout = requireRole(authenticatedEmail, UserRole.OJEADOR_ENTRENADOR);
        UserAccount athlete = requireAthlete(request.athleteId());
        Instant now = clock.instant();
        if (request.proposedDate() != null && !request.proposedDate().isAfter(now)) {
            throw ApiException.invalid("La fecha propuesta debe ser posterior al momento actual.");
        }
        if (!scoutInterestRepository.existsByScout_IdAndAthlete_Id(scout.getId(), athlete.getId())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "INTEREST_REQUIRED",
                    "Marca al deportista como prospecto de interés antes de enviar la solicitud.");
        }
        if (matchRequestRepository.existsByScout_IdAndAthlete_IdAndStatus(scout.getId(), athlete.getId(),
                MatchRequestStatus.PENDIENTE)) {
            throw new ApiException(HttpStatus.CONFLICT, "REQUEST_ALREADY_PENDING",
                    "Ya tienes una solicitud pendiente con este deportista.");
        }
        MatchRequest saved = matchRequestRepository.save(
                new MatchRequest(athlete, scout, request.message().trim(), request.proposedDate(), now));
        notificationService.notify(athlete, NotificationType.REQUEST_CREATED,
                profileDirectory.scoutDisplayName(scout) + " te envió una solicitud de contacto.");
        return toResponse(saved, scout);
    }

    /** Received requests for an athlete, sent ones for a scout; newest first. */
    @Transactional(readOnly = true)
    public List<MatchRequestResponse> listOwn(String authenticatedEmail, MatchRequestStatus status) {
        UserAccount viewer = requireAccount(authenticatedEmail);
        List<MatchRequest> requests = switch (viewer.getRole()) {
            case DEPORTISTA -> matchRequestRepository.findByAthlete_IdOrderByCreatedAtDesc(viewer.getId());
            case OJEADOR_ENTRENADOR -> matchRequestRepository.findByScout_IdOrderByCreatedAtDesc(viewer.getId());
            default -> throw ApiException.forbidden("Tu rol no tiene solicitudes de contacto.");
        };
        return requests.stream()
                .filter(request -> status == null || request.getStatus() == status)
                .map(request -> toResponse(request, viewer))
                .toList();
    }

    @Transactional
    public MatchRequestResponse respond(UUID requestId, MatchRequestRespondRequest body, String authenticatedEmail) {
        UserAccount athlete = requireRole(authenticatedEmail, UserRole.DEPORTISTA);
        MatchRequest request = matchRequestRepository.findById(requestId)
                .orElseThrow(() -> ApiException.notFound("No se encontró la solicitud."));
        if (!request.getAthlete().getId().equals(athlete.getId())) {
            throw ApiException.forbidden("No eres el destinatario de esta solicitud.");
        }
        if (request.getStatus() != MatchRequestStatus.PENDIENTE) {
            throw new ApiException(HttpStatus.CONFLICT, "REQUEST_ALREADY_ANSWERED",
                    "Esta solicitud ya fue respondida.");
        }
        Instant now = clock.instant();
        if (body.decision() == MatchRequestRespondRequest.Decision.ACCEPTED) {
            request.accept(now);
            notificationService.notify(request.getScout(), NotificationType.REQUEST_ACCEPTED,
                    athlete.getFullName() + " aceptó tu solicitud. Ya puedes ver sus datos de contacto.");
        } else {
            request.reject(now);
            notificationService.notify(request.getScout(), NotificationType.REQUEST_REJECTED,
                    athlete.getFullName() + " rechazó tu solicitud.");
        }
        return toResponse(request, athlete);
    }

    private MatchRequestResponse toResponse(MatchRequest request, UserAccount viewer) {
        UserAccount athlete = request.getAthlete();
        UserAccount scout = request.getScout();
        Optional<AthleteProfile> athleteProfile = profileDirectory.athleteProfile(athlete.getId());
        Optional<ScoutProfile> scoutProfile = profileDirectory.scoutProfile(scout.getId());

        MatchRequestResponse.Contact contact = null;
        if (request.getStatus() == MatchRequestStatus.ACEPTADA) {
            contact = viewer.getId().equals(athlete.getId())
                    ? new MatchRequestResponse.Contact(scoutProfile.map(ScoutProfile::getContactPhone).orElse(null),
                    scout.getEmail())
                    : athleteContact(athlete);
        }
        return new MatchRequestResponse(
                request.getId(), athlete.getId(), scout.getId(), request.getMessage(), request.getProposedDate(),
                request.getStatus(), request.getContactSharedAt(), request.getCreatedAt(),
                new MatchRequestResponse.AthleteSummary(athlete.getId(), athlete.getFullName(),
                        athleteProfile.map(AthleteProfile::getSport).orElse(null),
                        athleteProfile.map(AthleteProfile::getPosition).orElse(null),
                        athleteProfile.map(AthleteProfile::getRegion).orElse(null)),
                new MatchRequestResponse.ScoutSummary(scout.getId(), scout.getFullName(),
                        scoutProfile.map(ScoutProfile::getClub).orElse(null)),
                contact);
    }

    /** Minors are reached through the guardian registered at sign-up. */
    private MatchRequestResponse.Contact athleteContact(UserAccount athlete) {
        if (profileDirectory.isMinor(athlete) && athlete.getGuardianEmail() != null) {
            return new MatchRequestResponse.Contact(null, athlete.getGuardianEmail());
        }
        return new MatchRequestResponse.Contact(null, athlete.getEmail());
    }

    private UserAccount requireAccount(String email) {
        return userAccountRepository.findByEmail(email).orElseThrow(ApiException::unauthenticated);
    }

    private UserAccount requireRole(String email, UserRole role) {
        UserAccount account = requireAccount(email);
        if (account.getRole() != role) {
            throw ApiException.forbidden("Tu rol no tiene permiso para acceder a este recurso.");
        }
        return account;
    }

    private UserAccount requireAthlete(UUID athleteId) {
        return userAccountRepository.findById(athleteId)
                .filter(account -> account.getRole() == UserRole.DEPORTISTA)
                .orElseThrow(() -> ApiException.notFound("No se encontró el perfil del deportista."));
    }
}
