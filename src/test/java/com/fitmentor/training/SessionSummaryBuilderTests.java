package com.fitmentor.training;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Builder: step-by-step construction with invariants checked once in {@code build()}. */
class SessionSummaryBuilderTests {

    @Test
    void buildsAnImmutableSummaryWithGeneratedId() {
        Map<String, Double> angles = new HashMap<>(Map.of("knee", 95.0));

        SessionSummary summary = SessionSummary.builder()
            .exercise("squat").reps(10).accuracy(90).calories(12.5).durationSeconds(300)
            .avgAngles(angles).createdAt(Instant.EPOCH).build();
        angles.put("hip", 100.0);

        assertThat(summary.id()).isNotNull();
        assertThat(summary.avgAngles()).containsOnlyKeys("knee");
        assertThatThrownBy(() -> summary.avgAngles().put("x", 1.0)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsInconsistentSummaries() {
        assertThatThrownBy(() -> SessionSummary.builder().exercise("squat").createdAt(Instant.EPOCH).reps(-1).build())
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> SessionSummary.builder().exercise("squat").createdAt(Instant.EPOCH).accuracy(101).build())
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> SessionSummary.builder().createdAt(Instant.EPOCH).build())
            .isInstanceOf(NullPointerException.class);
    }
}
