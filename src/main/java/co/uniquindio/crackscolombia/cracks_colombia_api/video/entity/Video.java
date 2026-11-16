package co.uniquindio.crackscolombia.cracks_colombia_api.video.entity;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "skill_videos", indexes = {
        @Index(name = "idx_skill_videos_athlete_uploaded", columnList = "athlete_id, uploaded_at"),
        @Index(name = "idx_skill_videos_status_uploaded", columnList = "status, uploaded_at")
})
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "athlete_id", nullable = false)
    private UserAccount athlete;

    @Column(name = "storage_key", nullable = false, unique = true)
    private UUID storageKey;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private VideoStatus status;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderated_by_id")
    private UserAccount moderatedBy;

    @Column(name = "moderated_at")
    private Instant moderatedAt;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    protected Video() {
    }

    public Video(UserAccount athlete, UUID storageKey, long sizeBytes,
                 int durationSeconds, Instant uploadedAt) {
        this.athlete = athlete;
        this.storageKey = storageKey;
        this.sizeBytes = sizeBytes;
        this.durationSeconds = durationSeconds;
        this.status = VideoStatus.PENDIENTE;
        this.uploadedAt = uploadedAt;
    }

    public UUID getId() {
        return id;
    }

    public UserAccount getAthlete() {
        return athlete;
    }

    public UUID getStorageKey() {
        return storageKey;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public VideoStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public UserAccount getModeratedBy() {
        return moderatedBy;
    }

    public Instant getModeratedAt() {
        return moderatedAt;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    /** Publishes a pending video. The caller must already have checked that it is still pending. */
    public void markApproved(UserAccount moderator, Instant moderatedAt) {
        ensureStillPending();
        this.status = VideoStatus.APROBADO;
        this.rejectionReason = null;
        this.moderatedBy = moderator;
        this.moderatedAt = moderatedAt;
    }

    /** Rejects a pending video with a reason the athlete can read. */
    public void markRejected(UserAccount moderator, String reason, Instant moderatedAt) {
        ensureStillPending();
        this.status = VideoStatus.RECHAZADO;
        this.rejectionReason = reason;
        this.moderatedBy = moderator;
        this.moderatedAt = moderatedAt;
    }

    private void ensureStillPending() {
        if (status != VideoStatus.PENDIENTE) {
            throw new IllegalStateException("Solo un video pendiente puede moderarse.");
        }
    }
}
