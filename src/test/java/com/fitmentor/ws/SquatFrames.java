package com.fitmentor.ws;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Synthetic MediaPipe-style squat frames (normalized coordinates) with a given knee angle. */
final class SquatFrames {

    static final int WIDTH = 1280;
    static final int HEIGHT = 720;

    private SquatFrames() {
    }

    /** Knee angles of one repetition: standing, going down, bottom, and standing again. */
    static List<Double> oneRepetition() {
        List<Double> angles = new ArrayList<>();
        for (int i = 0; i < 6; i++) angles.add(175.0);
        for (double angle = 160; angle >= 90; angle -= 14) angles.add(angle);
        for (int i = 0; i < 6; i++) angles.add(88.0);
        for (double angle = 100; angle <= 175; angle += 15) angles.add(angle);
        for (int i = 0; i < 6; i++) angles.add(175.0);
        return angles;
    }

    /** STOMP {@code /app/pose} body for a frame whose left knee forms {@code kneeAngle} degrees. */
    static Map<String, Object> frame(String sessionId, double kneeAngle) {
        double hipX = 640, hipY = 300, kneeX = 640, kneeY = 450, shin = 150;
        double radians = Math.toRadians(kneeAngle);
        double ankleX = kneeX + shin * Math.sin(radians);
        double ankleY = kneeY - shin * Math.cos(radians);
        return Map.of(
            "session_id", sessionId,
            "exercise", "squat",
            "frame_width", WIDTH,
            "frame_height", HEIGHT,
            "points", List.of(
                point("left_shoulder", 640, 150),
                point("left_hip", hipX, hipY),
                point("left_knee", kneeX, kneeY),
                point("left_ankle", ankleX, ankleY)));
    }

    private static Map<String, Object> point(String name, double x, double y) {
        return Map.of("name", name, "x", x / WIDTH, "y", y / HEIGHT, "confidence", 0.95);
    }
}
