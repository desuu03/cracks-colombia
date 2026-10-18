package co.uniquindio.crackscolombia.cracks_colombia_api.match.repository;

import co.uniquindio.crackscolombia.cracks_colombia_api.match.entity.MatchRequest;
import co.uniquindio.crackscolombia.cracks_colombia_api.match.entity.MatchRequestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MatchRequestRepository extends JpaRepository<MatchRequest, UUID> {

    @EntityGraph(attributePaths = {"athlete", "scout"})
    List<MatchRequest> findByAthlete_IdOrderByCreatedAtDesc(UUID athleteId);

    @EntityGraph(attributePaths = {"athlete", "scout"})
    List<MatchRequest> findByScout_IdOrderByCreatedAtDesc(UUID scoutId);

    boolean existsByScout_IdAndAthlete_IdAndStatus(UUID scoutId, UUID athleteId, MatchRequestStatus status);
}
