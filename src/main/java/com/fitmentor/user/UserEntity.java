package com.fitmentor.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Row of the {@code users} table (same shape as the FITMENTOR-BD v2 schema). */
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "google_id", unique = true, length = 64)
    private String googleId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 180)
    private String email;

    @Column(name = "picture", columnDefinition = "text")
    private String picture;

    @Column(name = "level", nullable = false, length = 30)
    private String level;

    @Column(name = "goal", length = 60)
    private String goal;

    @Column(name = "is_guest", nullable = false)
    private boolean guest;

    /** BCrypt hash for email/password sign-in. Null for Google and guest users. Never exposed in responses. */
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    /** Tokens issued before this instant are rejected. Set when Google links an email/password account. */
    @Column(name = "sessions_valid_after")
    private Instant sessionsValidAfter;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserEntity() {
    }

    public UserEntity(UUID id, String name, String email, boolean guest, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.guest = guest;
        this.level = "BEGINNER";
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPicture() { return picture; }
    public void setPicture(String picture) { this.picture = picture; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }
    public boolean isGuest() { return guest; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Instant getSessionsValidAfter() { return sessionsValidAfter; }
    public void setSessionsValidAfter(Instant sessionsValidAfter) { this.sessionsValidAfter = sessionsValidAfter; }

    /**
     * Value embedded in every token ({@code svm} claim). Tokens carry the marker that was current
     * when they were issued; they are valid only while it still matches. 0 means never revoked.
     */
    public long sessionMarker() {
        return sessionsValidAfter == null ? 0L : sessionsValidAfter.toEpochMilli();
    }
    public Instant getCreatedAt() { return createdAt; }
}
