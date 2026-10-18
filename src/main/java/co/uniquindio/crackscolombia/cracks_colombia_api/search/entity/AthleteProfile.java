package co.uniquindio.crackscolombia.cracks_colombia_api.search.entity;

import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(name = "athlete_profiles", uniqueConstraints = {
        @UniqueConstraint(name = "uk_athlete_profiles_athlete", columnNames = "athlete_id")
}, indexes = {
        @Index(name = "idx_athlete_profiles_status_sport_position_region",
                columnList = "status, sport, position, region"),
        @Index(name = "idx_athlete_profiles_status_region", columnList = "status, region")
})
public class AthleteProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "athlete_id", nullable = false, unique = true)
    private UserAccount athlete;

    @Column(length = 80)
    private String sport;

    @Column(length = 80)
    private String position;

    @Column(length = 100)
    private String region;

    @Column(name = "photo_storage_key", unique = true)
    private UUID photoStorageKey;

    @Column(name = "photo_content_type", length = 32)
    private String photoContentType;

    @Column(name = "photo_size_bytes")
    private Long photoSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AthleteProfileStatus status = AthleteProfileStatus.PENDIENTE;

    protected AthleteProfile() {
    }

    public AthleteProfile(UserAccount athlete, String sport, String position, String region,
                          AthleteProfileStatus status) {
        this.athlete = athlete;
        this.sport = sport;
        this.position = position;
        this.region = region;
        this.status = status == null ? AthleteProfileStatus.PENDIENTE : status;
    }

    public void setPhoto(UUID photoStorageKey, String photoContentType, long photoSizeBytes) {
        this.photoStorageKey = photoStorageKey;
        this.photoContentType = photoContentType;
        this.photoSizeBytes = photoSizeBytes;
    }

    public UUID getId() {
        return id;
    }

    public UserAccount getAthlete() {
        return athlete;
    }

    public String getSport() {
        return sport;
    }

    public String getPosition() {
        return position;
    }

    public String getRegion() {
        return region;
    }

    public UUID getPhotoStorageKey() {
        return photoStorageKey;
    }

    public String getPhotoContentType() {
        return photoContentType;
    }

    public Long getPhotoSizeBytes() {
        return photoSizeBytes;
    }

    public AthleteProfileStatus getStatus() {
        return status;
    }
}
