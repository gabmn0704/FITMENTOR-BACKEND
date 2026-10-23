package com.fitmentor.training;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Summary of one saved training session, returned by the API.
 *
 * <p>Builder pattern: the object has many optional fields and must be consistent when it
 * is created (non-negative counts, accuracy in 0-100, a creation instant). Instances are
 * immutable once built; {@link Builder#build()} validates the invariants.
 */
public record SessionSummary(
    UUID id,
    String exercise,
    int reps,
    double accuracy,
    double calories,
    int durationSeconds,
    Map<String, Double> avgAngles,
    Instant createdAt
) {

    public SessionSummary {
        avgAngles = Map.copyOf(avgAngles);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Step-by-step construction of a {@link SessionSummary}. */
    public static final class Builder {
        private UUID id;
        private String exercise;
        private int reps;
        private double accuracy;
        private double calories;
        private int durationSeconds;
        private Map<String, Double> avgAngles = Map.of();
        private Instant createdAt;

        private Builder() {
        }

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder exercise(String exercise) { this.exercise = exercise; return this; }
        public Builder reps(int reps) { this.reps = reps; return this; }
        public Builder accuracy(double accuracy) { this.accuracy = accuracy; return this; }
        public Builder calories(double calories) { this.calories = calories; return this; }
        public Builder durationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        /** Optional joint averages; null is treated as "no angles". */
        public Builder avgAngles(Map<String, Double> avgAngles) {
            this.avgAngles = avgAngles == null ? Map.of() : new LinkedHashMap<>(avgAngles);
            return this;
        }

        public SessionSummary build() {
            Objects.requireNonNull(exercise, "exercise is required");
            Objects.requireNonNull(createdAt, "createdAt is required");
            if (reps < 0 || durationSeconds < 0 || calories < 0) {
                throw new IllegalStateException("reps, duration and calories must not be negative");
            }
            if (accuracy < 0 || accuracy > 100) {
                throw new IllegalStateException("accuracy must be between 0 and 100");
            }
            return new SessionSummary(
                id == null ? UUID.randomUUID() : id,
                exercise, reps, accuracy, calories, durationSeconds, avgAngles, createdAt);
        }
    }
}
