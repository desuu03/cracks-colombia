package co.uniquindio.crackscolombia.cracks_colombia_api.user.repository;

import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.AthleteProfileDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AthleteProfileDetailsRepository extends JpaRepository<AthleteProfileDetails, UUID> {

    Optional<AthleteProfileDetails> findByAthlete_Id(UUID athleteId);
}
