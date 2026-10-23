package com.fitmentor.training;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

/** Row of {@code user_achievements}: an achievement unlocked by a user, with its unlock time. */
@Entity
@Table(name = "user_achievements")
public class UserAchievementEntity {

    @EmbeddedId
    private UserAchievementId id;

    @Column(name = "unlocked_at", nullable = false)
    private Instant unlockedAt;

    protected UserAchievementEntity() {
    }

    public UserAchievementEntity(UserAchievementId id, Instant unlockedAt) {
        this.id = id;
        this.unlockedAt = unlockedAt;
    }

    public UserAchievementId getId() { return id; }
    public Instant getUnlockedAt() { return unlockedAt; }
}
