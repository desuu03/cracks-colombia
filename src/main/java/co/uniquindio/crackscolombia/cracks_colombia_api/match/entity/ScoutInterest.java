package co.uniquindio.crackscolombia.cracks_colombia_api.match.entity;

import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/** A scout marked an athlete as a prospect; required before sending a match request. */
@Entity
@Table(name = "scout_interests", uniqueConstraints = {
        @UniqueConstraint(name = "uk_scout_interests_scout_athlete", columnNames = {"scout_id", "athlete_id"})
})
public class ScoutInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scout_id", nullable = false)
    private UserAccount scout;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "athlete_id", nullable = false)
    private UserAccount athlete;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ScoutInterest() {
    }

    public ScoutInterest(UserAccount scout, UserAccount athlete, Instant createdAt) {
        this.scout = scout;
        this.athlete = athlete;
        this.createdAt = createdAt;
    }
}
