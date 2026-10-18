package co.uniquindio.crackscolombia.cracks_colombia_api.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * Physical data and sporting history of an athlete. Sport, position and region
 * live in {@code athlete_profiles}, which the search reads; this table holds the
 * rest of the profile without changing that entity.
 */
@Entity
@Table(name = "athlete_profile_details", uniqueConstraints = {
        @UniqueConstraint(name = "uk_athlete_profile_details_athlete", columnNames = "athlete_id")
})
public class AthleteProfileDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "athlete_id", nullable = false, unique = true)
    private UserAccount athlete;

    @Column(name = "height_cm")
    private Integer heightCm;

    @Column(name = "weight_kg")
    private Integer weightKg;

    @Column(length = 1000)
    private String trajectory;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AthleteProfileDetails() {
    }

    public AthleteProfileDetails(UserAccount athlete) {
        this.athlete = athlete;
    }

    public void update(Integer heightCm, Integer weightKg, String trajectory, Instant updatedAt) {
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.trajectory = trajectory;
        this.updatedAt = updatedAt;
    }

    public Integer getHeightCm() {
        return heightCm;
    }

    public Integer getWeightKg() {
        return weightKg;
    }

    public String getTrajectory() {
        return trajectory;
    }
}
