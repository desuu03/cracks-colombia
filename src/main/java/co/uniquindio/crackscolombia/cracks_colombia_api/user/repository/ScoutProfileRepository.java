package co.uniquindio.crackscolombia.cracks_colombia_api.user.repository;

import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.ScoutProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ScoutProfileRepository extends JpaRepository<ScoutProfile, UUID> {

    Optional<ScoutProfile> findByScout_Id(UUID scoutId);
}
