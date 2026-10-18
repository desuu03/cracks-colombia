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

/** Club and phone of a scout or coach, shared with athletes who accept a request. */
@Entity
@Table(name = "scout_profiles", uniqueConstraints = {
        @UniqueConstraint(name = "uk_scout_profiles_scout", columnNames = "scout_id")
})
public class ScoutProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scout_id", nullable = false, unique = true)
    private UserAccount scout;

    @Column(length = 120)
    private String club;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ScoutProfile() {
    }

    public ScoutProfile(UserAccount scout) {
        this.scout = scout;
    }

    public void update(String club, String contactPhone, Instant updatedAt) {
        this.club = club;
        this.contactPhone = contactPhone;
        this.updatedAt = updatedAt;
    }

    public String getClub() {
        return club;
    }

    public String getContactPhone() {
        return contactPhone;
    }
}
