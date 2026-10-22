package com.fitmentor.domain.pose;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * One camera frame sent by the client (STOMP {@code /app/pose}) and forwarded to the AI
 * service. JSON names follow the frontend and AI contract.
 */
public record PoseFrame(
    List<PosePoint> points,
    String exercise,
    @JsonProperty("frame_width") int frameWidth,
    @JsonProperty("frame_height") int frameHeight,
    @JsonProperty("session_id") String sessionId
) {
    public PoseFrame {
        points = points == null ? List.of() : List.copyOf(points);
    }
}
