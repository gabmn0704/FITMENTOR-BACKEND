package com.fitmentor.domain.pose;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MediaPipePoseStrategyTests {

    private final MediaPipePoseStrategy strategy = new MediaPipePoseStrategy();

    @Test
    void goodSquatScoresOneHundredWithoutCounting() {
        PoseAnalysis result = strategy.analyze(squat(new PosePoint("left_ankle", 1, 1, 0.9), 0.9));

        assertThat(result.score()).isEqualTo(100.0);
        assertThat(result.feedback()).containsExactly("Buena postura, sigue así.");
        assertThat(result.reps()).isNull();
        assertThat(result.phase()).isNull();
        assertThat(result.engine()).isEqualTo("local");
    }

    @Test
    void straightLegSquatGetsKneeCorrectionAndLowerScore() {
        // Knee at 180 degrees: outside the 70-110 range of the squat rule.
        PoseAnalysis result = strategy.analyze(squat(new PosePoint("left_ankle", 2, 0, 0.9), 0.9));

        assertThat(result.score()).isEqualTo(50.0);
        assertThat(result.feedback()).containsExactly("Baja la cadera hasta que los muslos queden casi paralelos al suelo.");
    }

    @Test
    void lowConfidenceLandmarksAreIgnored() {
        PoseAnalysis result = strategy.analyze(squat(new PosePoint("left_ankle", 2, 0, 0.1), 0.9));

        assertThat(result.score()).isEqualTo(100.0);
    }

    @Test
    void reportsMissingLandmarksAndUnknownExercises() {
        PoseAnalysis missing = strategy.analyze(new PoseFrame(List.of(), "squat", 1280, 720, "s1"));
        PoseAnalysis unknown = strategy.analyze(new PoseFrame(List.of(), "burpee", 1280, 720, "s1"));

        assertThat(missing.score()).isZero();
        assertThat(missing.feedback()).containsExactly("No se detectan suficientes puntos del cuerpo.");
        assertThat(unknown.feedback()).containsExactly("Ejercicio no soportado.");
    }

    @Test
    void angleIsComputedAtTheVertex() {
        PosePoint vertex = new PosePoint("v", 0, 0, 1);

        assertThat(MediaPipePoseStrategy.angleAt(vertex, new PosePoint("a", 1, 0, 1), new PosePoint("b", 0, 1, 1)))
            .isCloseTo(90.0, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(MediaPipePoseStrategy.angleAt(vertex, new PosePoint("a", 1, 0, 1), new PosePoint("b", 2, 0, 1)))
            .isCloseTo(0.0, org.assertj.core.data.Offset.offset(1e-9));
    }

    /** Hip at origin, knee to the right, ankle below the knee: a 90 degree knee angle. */
    private static PoseFrame squat(PosePoint ankle, double confidence) {
        return new PoseFrame(
            List.of(
                new PosePoint("left_shoulder", 0, -1, confidence),
                new PosePoint("left_hip", 0, 0, confidence),
                new PosePoint("left_knee", 1, 0, confidence),
                ankle),
            "squat", 1280, 720, "session-1");
    }

}
