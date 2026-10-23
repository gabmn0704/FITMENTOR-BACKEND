package com.fitmentor.domain.achievement;

/** Evaluated achievement returned by {@code GET /api/achievements}. */
public record AchievementStatus(String id, String title, String description, boolean unlocked, int progress) {
}
