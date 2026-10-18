package co.uniquindio.crackscolombia.cracks_colombia_api.user.service;

import co.uniquindio.crackscolombia.cracks_colombia_api.search.entity.AthleteProfile;
import co.uniquindio.crackscolombia.cracks_colombia_api.search.entity.AthleteProfileStatus;
import co.uniquindio.crackscolombia.cracks_colombia_api.search.repository.AthleteProfileRepository;
import co.uniquindio.crackscolombia.cracks_colombia_api.shared.exception.ApiException;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.dto.request.UserProfileUpdateRequest;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.dto.response.MyProfileResponse;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.AthleteProfileDetails;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.ScoutProfile;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserAccount;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.repository.AthleteProfileDetailsRepository;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.repository.AthleteSportDataWriter;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.repository.ScoutProfileRepository;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserProfileService {

    private final UserAccountRepository userAccountRepository;
    private final AthleteProfileRepository athleteProfileRepository;
    private final AthleteSportDataWriter athleteSportDataWriter;
    private final AthleteProfileDetailsRepository detailsRepository;
    private final ScoutProfileRepository scoutProfileRepository;
    private final Clock clock;
    private final boolean autoApproveProfiles;

    /**
     * @param autoApproveProfiles search only lists APROBADO profiles and there is no
     *                            profile moderation yet, so complete profiles are approved
     *                            on save. Set app.profile.auto-approve=false to leave them
     *                            PENDIENTE for a moderator instead.
     */
    public UserProfileService(UserAccountRepository userAccountRepository,
                              AthleteProfileRepository athleteProfileRepository,
                              AthleteSportDataWriter athleteSportDataWriter,
                              AthleteProfileDetailsRepository detailsRepository,
                              ScoutProfileRepository scoutProfileRepository,
                              Clock clock,
                              @Value("${app.profile.auto-approve:true}") boolean autoApproveProfiles) {
        this.userAccountRepository = userAccountRepository;
        this.athleteProfileRepository = athleteProfileRepository;
        this.athleteSportDataWriter = athleteSportDataWriter;
        this.detailsRepository = detailsRepository;
        this.scoutProfileRepository = scoutProfileRepository;
        this.clock = clock;
        this.autoApproveProfiles = autoApproveProfiles;
    }

    @Transactional(readOnly = true)
    public MyProfileResponse getOwn(String authenticatedEmail) {
        return toResponse(requireAccount(authenticatedEmail));
    }

    @Transactional
    public MyProfileResponse updateOwn(String authenticatedEmail, UserProfileUpdateRequest request) {
        UserAccount account = requireAccount(authenticatedEmail);
        switch (account.getRole()) {
            case DEPORTISTA -> updateAthlete(account, request);
            case OJEADOR_ENTRENADOR -> updateScout(account, request);
            default -> throw ApiException.forbidden("Tu rol no tiene un perfil editable.");
        }
        // The sport update clears the persistence context; read everything back fresh.
        return toResponse(requireAccount(authenticatedEmail));
    }

    private void updateAthlete(UserAccount athlete, UserProfileUpdateRequest request) {
        String sport = trimToNull(request.sport());
        String position = trimToNull(request.position());
        String region = trimToNull(request.region());
        if (!SportsCatalog.isSport(sport)) {
            throw ApiException.invalid("Elige un deporte de la lista.");
        }
        if (!SportsCatalog.isPosition(sport, position)) {
            throw ApiException.invalid("Elige una posición o prueba de " + sport + ".");
        }
        if (!SportsCatalog.isRegion(region)) {
            throw ApiException.invalid("Elige un departamento de la lista.");
        }

        AthleteProfileDetails details = detailsRepository.findByAthlete_Id(athlete.getId())
                .orElseGet(() -> new AthleteProfileDetails(athlete));
        details.update(request.heightCm(), request.weightKg(), trimToNull(request.trajectory()), clock.instant());
        detailsRepository.save(details);

        AthleteProfileStatus status = autoApproveProfiles ? AthleteProfileStatus.APROBADO : AthleteProfileStatus.PENDIENTE;
        if (athleteProfileRepository.findByAthlete_Id(athlete.getId()).isPresent()) {
            athleteSportDataWriter.updateSportData(athlete.getId(), sport, position, region, status);
        } else {
            athleteProfileRepository.save(new AthleteProfile(athlete, sport, position, region, status));
        }
    }

    private void updateScout(UserAccount scout, UserProfileUpdateRequest request) {
        ScoutProfile profile = scoutProfileRepository.findByScout_Id(scout.getId())
                .orElseGet(() -> new ScoutProfile(scout));
        profile.update(trimToNull(request.club()), trimToNull(request.contactPhone()), clock.instant());
        scoutProfileRepository.save(profile);
    }

    private MyProfileResponse toResponse(UserAccount account) {
        UUID id = account.getId();
        Optional<AthleteProfile> profile = athleteProfileRepository.findByAthlete_Id(id);
        Optional<AthleteProfileDetails> details = detailsRepository.findByAthlete_Id(id);
        Optional<ScoutProfile> scout = scoutProfileRepository.findByScout_Id(id);
        return new MyProfileResponse(
                id, account.getEmail(), account.getFullName(), account.getRole(), account.getBirthDate(),
                profile.map(AthleteProfile::getSport).orElse(null),
                profile.map(AthleteProfile::getPosition).orElse(null),
                profile.map(AthleteProfile::getRegion).orElse(null),
                details.map(AthleteProfileDetails::getHeightCm).orElse(null),
                details.map(AthleteProfileDetails::getWeightKg).orElse(null),
                details.map(AthleteProfileDetails::getTrajectory).orElse(null),
                profile.map(AthleteProfile::getStatus).orElse(null),
                scout.map(ScoutProfile::getClub).orElse(null),
                scout.map(ScoutProfile::getContactPhone).orElse(null));
    }

    private UserAccount requireAccount(String email) {
        return userAccountRepository.findByEmail(email).orElseThrow(ApiException::unauthenticated);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
