package com.fitmentor.ws;

import java.util.List;

/** JSON payloads sent over STOMP, matching the frontend contract. */
public final class FeedbackPayloads {

    private FeedbackPayloads() {
    }

    /** Sent to {@code /topic/feedback.{session_id}}. */
    public record Feedback(
        String type,
        double score,
        String message,
        List<String> feedback,
        String phase,
        int reps,
        String state,
        String exercise
    ) {
    }

    /** Sent to {@code /topic/state.{session_id}}. */
    public record State(String type, String state, String exercise) {
    }
}
