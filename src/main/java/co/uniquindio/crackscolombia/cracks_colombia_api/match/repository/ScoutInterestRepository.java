package co.uniquindio.crackscolombia.cracks_colombia_api.match.repository;

import co.uniquindio.crackscolombia.cracks_colombia_api.match.entity.ScoutInterest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ScoutInterestRepository extends JpaRepository<ScoutInterest, UUID> {

    boolean existsByScout_IdAndAthlete_Id(UUID scoutId, UUID athleteId);
}
