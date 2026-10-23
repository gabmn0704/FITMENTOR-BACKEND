package com.fitmentor.domain.achievement;

/** Aggregates read by the achievement rules. */
public record AchievementInput(int totalSessions, int totalReps, int streakDays, double bestAccuracy, int distinctExercises) {
}
