package co.uniquindio.crackscolombia.cracks_colombia_api.search.repository;

import co.uniquindio.crackscolombia.cracks_colombia_api.search.dto.response.AthleteSearchCandidate;
import co.uniquindio.crackscolombia.cracks_colombia_api.search.entity.AthleteProfile;
import co.uniquindio.crackscolombia.cracks_colombia_api.search.entity.AthleteProfileStatus;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;

public interface AthleteProfileRepository extends JpaRepository<AthleteProfile, UUID> {

    java.util.Optional<AthleteProfile> findByAthlete_Id(UUID athleteId);

    @Query(value = """
            select new co.uniquindio.crackscolombia.cracks_colombia_api.search.dto.response.AthleteSearchCandidate(
                athlete.id, profile.photoStorageKey, athlete.fullName, profile.sport, profile.position,
                athlete.birthDate, profile.region)
            from AthleteProfile profile
            join profile.athlete athlete
            where profile.status = :status
              and athlete.role = :role
              and athlete.birthDate is not null
              and (cast(:sport as String) is null or lower(profile.sport) = lower(cast(:sport as String)))
              and (cast(:position as String) is null or lower(profile.position) = lower(cast(:position as String)))
              and (cast(:region as String) is null or lower(profile.region) = lower(cast(:region as String)))
              and athlete.birthDate >= :earliestBirthDate
              and athlete.birthDate <= :latestBirthDate
            order by athlete.fullName asc, athlete.id asc
            """,
            countQuery = """
            select count(profile)
            from AthleteProfile profile
            join profile.athlete athlete
            where profile.status = :status
              and athlete.role = :role
              and athlete.birthDate is not null
              and (cast(:sport as String) is null or lower(profile.sport) = lower(cast(:sport as String)))
              and (cast(:position as String) is null or lower(profile.position) = lower(cast(:position as String)))
              and (cast(:region as String) is null or lower(profile.region) = lower(cast(:region as String)))
              and athlete.birthDate >= :earliestBirthDate
              and athlete.birthDate <= :latestBirthDate
            """)
    Page<AthleteSearchCandidate> searchApprovedProfiles(
            @Param("status") AthleteProfileStatus status,
            @Param("role") UserRole role,
            @Param("sport") String sport,
            @Param("position") String position,
            @Param("region") String region,
            @Param("earliestBirthDate") LocalDate earliestBirthDate,
            @Param("latestBirthDate") LocalDate latestBirthDate,
            Pageable pageable
    );
}
