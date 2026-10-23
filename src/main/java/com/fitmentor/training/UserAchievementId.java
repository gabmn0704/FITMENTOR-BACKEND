package com.fitmentor.training;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Composite key of {@code user_achievements}: one unlock per user and achievement. */
@Embeddable
public class UserAchievementId implements Serializable {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "achievement_id", nullable = false, length = 60)
    private String achievementId;

    protected UserAchievementId() {
    }

    public UserAchievementId(UUID userId, String achievementId) {
        this.userId = userId;
        this.achievementId = achievementId;
    }

    public UUID getUserId() { return userId; }
    public String getAchievementId() { return achievementId; }

    @Override
    public boolean equals(Object other) {
        return other instanceof UserAchievementId that
            && Objects.equals(userId, that.userId)
            && Objects.equals(achievementId, that.achievementId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, achievementId);
    }
}
