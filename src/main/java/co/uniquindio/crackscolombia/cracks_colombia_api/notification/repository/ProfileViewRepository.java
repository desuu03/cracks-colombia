package co.uniquindio.crackscolombia.cracks_colombia_api.notification.repository;

import co.uniquindio.crackscolombia.cracks_colombia_api.notification.entity.ProfileView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.UUID;

public interface ProfileViewRepository extends JpaRepository<ProfileView, UUID> {

    boolean existsByScout_IdAndAthlete_IdAndViewedOn(UUID scoutId, UUID athleteId, LocalDate viewedOn);
}
