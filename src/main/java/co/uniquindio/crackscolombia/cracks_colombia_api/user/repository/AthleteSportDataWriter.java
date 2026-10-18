package co.uniquindio.crackscolombia.cracks_colombia_api.user.repository;

import co.uniquindio.crackscolombia.cracks_colombia_api.search.entity.AthleteProfile;
import co.uniquindio.crackscolombia.cracks_colombia_api.search.entity.AthleteProfileStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Updates the searchable fields of an existing {@link AthleteProfile}. The entity
 * belongs to the search module and exposes no setters for them, so the profile
 * module writes through this query instead of changing it.
 */
public interface AthleteSportDataWriter extends Repository<AthleteProfile, UUID> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update AthleteProfile profile
            set profile.sport = :sport, profile.position = :position, profile.region = :region,
                profile.status = :status
            where profile.athlete.id = :athleteId
            """)
    int updateSportData(@Param("athleteId") UUID athleteId,
                        @Param("sport") String sport,
                        @Param("position") String position,
                        @Param("region") String region,
                        @Param("status") AthleteProfileStatus status);
}
