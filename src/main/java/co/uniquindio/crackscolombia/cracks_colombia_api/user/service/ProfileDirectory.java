package co.uniquindio.crackscolombia.cracks_colombia_api.user.service;

import co.uniquindio.crackscolombia.cracks_colombia_api.search.entity.AthleteProfile;
import co.uniquindio.crackscolombia.cracks_colombia_api.search.repository.AthleteProfileRepository;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.ScoutProfile;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserAccount;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.repository.ScoutProfileRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;
import java.util.UUID;

/** Read helpers shared by the profile and match modules. */
@Component
public class ProfileDirectory {

    private static final int AGE_OF_MAJORITY = 18;

    private final AthleteProfileRepository athleteProfileRepository;
    private final ScoutProfileRepository scoutProfileRepository;
    private final Clock clock;

    public ProfileDirectory(AthleteProfileRepository athleteProfileRepository,
                            ScoutProfileRepository scoutProfileRepository,
                            Clock clock) {
        this.athleteProfileRepository = athleteProfileRepository;
        this.scoutProfileRepository = scoutProfileRepository;
        this.clock = clock;
    }

    public Optional<AthleteProfile> athleteProfile(UUID athleteId) {
        return athleteProfileRepository.findByAthlete_Id(athleteId);
    }

    public Optional<ScoutProfile> scoutProfile(UUID scoutId) {
        return scoutProfileRepository.findByScout_Id(scoutId);
    }

    /** The club when the scout registered one, otherwise their name. */
    public String scoutDisplayName(UserAccount scout) {
        return scoutProfile(scout.getId())
                .map(ScoutProfile::getClub)
                .filter(club -> !club.isBlank())
                .orElse(scout.getFullName());
    }

    public Integer ageOf(UserAccount account) {
        LocalDate birthDate = account.getBirthDate();
        return birthDate == null ? null : Period.between(birthDate, LocalDate.now(clock)).getYears();
    }

    public boolean isMinor(UserAccount account) {
        Integer age = ageOf(account);
        return age != null && age < AGE_OF_MAJORITY;
    }
}
