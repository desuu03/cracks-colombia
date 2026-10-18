package co.uniquindio.crackscolombia.cracks_colombia_api.match.entity;

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

/** A scout's request to contact an athlete or invite them to a trial. */
@Entity
@Table(name = "match_requests", indexes = {
        @Index(name = "idx_match_requests_athlete_created", columnList = "athlete_id, created_at"),
        @Index(name = "idx_match_requests_scout_created", columnList = "scout_id, created_at")
})
public class MatchRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "athlete_id", nullable = false)
    private UserAccount athlete;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scout_id", nullable = false)
    private UserAccount scout;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "proposed_date")
    private Instant proposedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MatchRequestStatus status = MatchRequestStatus.PENDIENTE;

    @Column(name = "contact_shared_at")
    private Instant contactSharedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    protected MatchRequest() {
    }

    public MatchRequest(UserAccount athlete, UserAccount scout, String message, Instant proposedDate,
                        Instant createdAt) {
        this.athlete = athlete;
        this.scout = scout;
        this.message = message;
        this.proposedDate = proposedDate;
        this.createdAt = createdAt;
    }

    public void accept(Instant now) {
        this.status = MatchRequestStatus.ACEPTADA;
        this.contactSharedAt = now;
        this.respondedAt = now;
    }

    public void reject(Instant now) {
        this.status = MatchRequestStatus.RECHAZADA;
        this.respondedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UserAccount getAthlete() {
        return athlete;
    }

    public UserAccount getScout() {
        return scout;
    }

    public String getMessage() {
        return message;
    }

    public Instant getProposedDate() {
        return proposedDate;
    }

    public MatchRequestStatus getStatus() {
        return status;
    }

    public Instant getContactSharedAt() {
        return contactSharedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
