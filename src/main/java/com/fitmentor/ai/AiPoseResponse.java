package com.fitmentor.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/** Response of the AI service {@code POST /analyze} (see the FITMENTOR-IA contract). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiPoseResponse(
    double score,
    List<String> feedback,
    Map<String, Double> angles,
    Double confidence,
    boolean analyzed,
    String phase,
    Integer reps,
    String message
) {
    public AiPoseResponse {
        feedback = feedback == null ? List.of() : List.copyOf(feedback);
    }
}
