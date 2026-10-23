package com.fitmentor.domain.session;

import java.time.Instant;

/**
 * Immutable view of a saved training session, used by the stats, routine and achievement
 * calculations. It is independent of JPA so these rules can be unit-tested.
 */
public record CompletedSession(
    String exercise,
    int reps,
    double accuracy,
    double calories,
    int durationSeconds,
    Instant createdAt
) {
}
