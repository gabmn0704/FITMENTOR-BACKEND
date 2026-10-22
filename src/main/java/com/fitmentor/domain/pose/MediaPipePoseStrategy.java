package com.fitmentor.domain.pose;

import com.fitmentor.domain.exercise.AngleRule;
import com.fitmentor.domain.exercise.Exercise;
import com.fitmentor.domain.exercise.ExerciseFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Strategy: local, offline pose analyzer used when the AI service does not answer.
 *
 * <p>It computes the joint angles of the frame and checks them against the posture rules
 * of the exercise. The score is the percentage of evaluated rules that are satisfied.
 * It never counts repetitions, because only the AI service has the temporal context.
 */
@Component
public class MediaPipePoseStrategy implements PoseAnalysisStrategy {

    public static final String ENGINE = "local";

    static final double MIN_CONFIDENCE = 0.3;

    @Override
    public PoseAnalysis analyze(PoseFrame frame) {
        Optional<Exercise> exercise = ExerciseFactory.create(frame.exercise());
        if (exercise.isEmpty()) {
            return noAnalysis("Ejercicio no soportado.");
        }
        Map<String, PosePoint> points = indexByName(frame.points());
        int evaluated = 0;
        int satisfied = 0;
        java.util.ArrayList<String> corrections = new java.util.ArrayList<>();
        for (AngleRule rule : exercise.get().postureRules()) {
            PosePoint first = visible(points, rule.first());
            PosePoint vertex = visible(points, rule.vertex());
            PosePoint last = visible(points, rule.last());
            if (first == null || vertex == null || last == null) {
                continue;
            }
            double angle = angleAt(vertex, first, last);
            if (Double.isNaN(angle)) {
                continue;
            }
            evaluated++;
            if (angle >= rule.minDegrees() && angle <= rule.maxDegrees()) {
                satisfied++;
            } else {
                corrections.add(rule.correction());
            }
        }
        if (evaluated == 0) {
            return noAnalysis("No se detectan suficientes puntos del cuerpo.");
        }
        double score = Math.round(100.0 * satisfied / evaluated);
        List<String> feedback = corrections.isEmpty() ? List.of("Buena postura, sigue así.") : corrections;
        return new PoseAnalysis(score, feedback, null, null, ENGINE);
    }

    @Override
    public String engine() {
        return ENGINE;
    }

    /** Angle in degrees at {@code vertex} formed by the segments to {@code first} and {@code last}. */
    static double angleAt(PosePoint vertex, PosePoint first, PosePoint last) {
        double ax = first.x() - vertex.x();
        double ay = first.y() - vertex.y();
        double bx = last.x() - vertex.x();
        double by = last.y() - vertex.y();
        double norm = Math.hypot(ax, ay) * Math.hypot(bx, by);
        if (norm == 0) {
            return Double.NaN;
        }
        double cos = Math.max(-1.0, Math.min(1.0, (ax * bx + ay * by) / norm));
        return Math.toDegrees(Math.acos(cos));
    }

    private static PosePoint visible(Map<String, PosePoint> points, String name) {
        PosePoint point = points.get(name);
        return point != null && point.confidence() >= MIN_CONFIDENCE ? point : null;
    }

    private static Map<String, PosePoint> indexByName(List<PosePoint> points) {
        Map<String, PosePoint> indexed = new HashMap<>();
        for (PosePoint point : points) {
            indexed.putIfAbsent(point.name(), point);
        }
        return indexed;
    }

    private static PoseAnalysis noAnalysis(String message) {
        return new PoseAnalysis(0, List.of(message), null, null, ENGINE);
    }
}
