package com.fitmentor.domain.pose;

import java.util.List;

/**
 * Result of analyzing one frame, whichever strategy produced it.
 *
 * @param phase exercise phase reported by the AI service, null for the local analyzer.
 * @param reps  repetitions reported by the AI service, null for the local analyzer
 *              (the local analyzer never counts repetitions).
 */
public record PoseAnalysis(double score, List<String> feedback, String phase, Integer reps, String engine) {

    public PoseAnalysis {
        feedback = feedback == null ? List.of() : List.copyOf(feedback);
    }
}
