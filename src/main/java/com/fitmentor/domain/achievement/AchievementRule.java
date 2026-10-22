package com.fitmentor.domain.achievement;

/**
 * Strategy-like rule: each achievement computes its own progress (0-100) from the input.
 * An achievement is unlocked when its progress reaches 100.
 */
public interface AchievementRule {
    String id();
    String title();
    String description();
    int progress(AchievementInput input);
}
