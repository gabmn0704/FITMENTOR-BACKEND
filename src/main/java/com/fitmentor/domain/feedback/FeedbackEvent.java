package com.fitmentor.domain.feedback;

import java.util.List;

/**
 * Posture feedback for one analyzed frame.
 *
 * @param phase exercise phase from the AI service, null for local analysis.
 * @param reps  repetitions counted so far in the session.
 */
public record FeedbackEvent(
    String sessionId,
    String message,
    double score,
    List<String> feedback,
    String phase,
    int reps,
    String state,
    String exercise
) implements SessionEvent {

    public FeedbackEvent {
        feedback = feedback == null ? List.of() : List.copyOf(feedback);
    }
}
