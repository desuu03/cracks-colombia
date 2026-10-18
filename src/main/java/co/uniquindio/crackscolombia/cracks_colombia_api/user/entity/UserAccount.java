package co.uniquindio.crackscolombia.cracks_colombia_api.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "user_accounts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_accounts_email", columnNames = "email")
}, indexes = {
        @Index(name = "idx_user_accounts_role_birth_date", columnList = "role, birth_date")
})
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 512)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserRole role;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "guardian_full_name", length = 150)
    private String guardianFullName;

    @Column(name = "guardian_email", length = 254)
    private String guardianEmail;

    @Column(name = "guardian_consent_accepted", nullable = false)
    private boolean guardianConsentAccepted;

    @Column(name = "guardian_consent_accepted_at")
    private Instant guardianConsentAcceptedAt;

    @Column(name = "data_processing_consent_accepted", nullable = false)
    private boolean dataProcessingConsentAccepted;

    @Column(name = "data_processing_consent_accepted_at", nullable = false)
    private Instant dataProcessingConsentAcceptedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserAccount() {
    }

    public UserAccount(String email, String passwordHash, String fullName, UserRole role,
                       LocalDate birthDate, String guardianFullName, String guardianEmail,
                       boolean guardianConsentAccepted, Instant guardianConsentAcceptedAt,
                       Instant dataProcessingConsentAcceptedAt) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.birthDate = birthDate;
        this.guardianFullName = guardianFullName;
        this.guardianEmail = guardianEmail;
        this.guardianConsentAccepted = guardianConsentAccepted;
        this.guardianConsentAcceptedAt = guardianConsentAcceptedAt;
        this.dataProcessingConsentAccepted = true;
        this.dataProcessingConsentAcceptedAt = dataProcessingConsentAcceptedAt;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public UserRole getRole() {
        return role;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getGuardianFullName() {
        return guardianFullName;
    }

    public String getGuardianEmail() {
        return guardianEmail;
    }

    public boolean isGuardianConsentAccepted() {
        return guardianConsentAccepted;
    }

    public Instant getGuardianConsentAcceptedAt() {
        return guardianConsentAcceptedAt;
    }

    public boolean isDataProcessingConsentAccepted() {
        return dataProcessingConsentAccepted;
    }

    public Instant getDataProcessingConsentAcceptedAt() {
        return dataProcessingConsentAcceptedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
