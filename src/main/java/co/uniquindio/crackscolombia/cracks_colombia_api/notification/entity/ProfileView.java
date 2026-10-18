package co.uniquindio.crackscolombia.cracks_colombia_api.notification.entity;

import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

/** A scout opened an athlete's profile on a given day; limits PROFILE_VIEWED to one per day. */
@Entity
@Table(name = "profile_views", indexes = {
        @Index(name = "idx_profile_views_scout_athlete_day", columnList = "scout_id, athlete_id, viewed_on")
})
public class ProfileView {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scout_id", nullable = false)
    private UserAccount scout;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "athlete_id", nullable = false)
    private UserAccount athlete;

    @Column(name = "viewed_on", nullable = false)
    private LocalDate viewedOn;

    protected ProfileView() {
    }

    public ProfileView(UserAccount scout, UserAccount athlete, LocalDate viewedOn) {
        this.scout = scout;
        this.athlete = athlete;
        this.viewedOn = viewedOn;
    }
}
