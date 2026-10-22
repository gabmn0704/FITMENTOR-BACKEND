package com.fitmentor.domain.pose.validation;

import com.fitmentor.domain.pose.PoseFrame;
import com.fitmentor.domain.pose.PosePoint;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class FrameValidationChainTests {

    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-10-21T12:00:00Z"));
    private final Clock clock = new Clock() {
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now.get(); }
    };
    private final FrameValidationChain chain = new FrameValidationChain(clock);

    @Test
    void acceptsWellFormedFrame() {
        assertThat(chain.validate(frame("s1", 0.9, 1280, 720)).accepted()).isTrue();
    }

    @Test
    void rejectsLowConfidenceBeforeAnythingElse() {
        FrameVerdict verdict = chain.validate(frame("s1", 0.1, 0, 0));

        assertThat(verdict.reason()).isEqualTo("low_confidence");
    }

    @Test
    void rejectsFramesCloserThanOneHundredFiftyMilliseconds() {
        assertThat(chain.validate(frame("s1", 0.9, 1280, 720)).accepted()).isTrue();

        now.set(now.get().plusMillis(149));
        assertThat(chain.validate(frame("s1", 0.9, 1280, 720)).reason()).isEqualTo("rate_limited");

        now.set(now.get().plusMillis(1));
        assertThat(chain.validate(frame("s1", 0.9, 1280, 720)).accepted()).isTrue();
    }

    @Test
    void rateLimitIsTrackedPerSession() {
        assertThat(chain.validate(frame("s1", 0.9, 1280, 720)).accepted()).isTrue();
        assertThat(chain.validate(frame("s2", 0.9, 1280, 720)).accepted()).isTrue();
    }

    @Test
    void rejectsInvalidSizeLast() {
        FrameVerdict verdict = chain.validate(frame("s2", 0.9, 0, 720));

        assertThat(verdict.reason()).isEqualTo("invalid_size");
    }

    @Test
    void rejectsTooManyPointsAfterSize() {
        List<PosePoint> points = java.util.stream.IntStream.range(0, SizeHandler.MAX_POINTS + 1)
            .mapToObj(i -> new PosePoint("p" + i, 0, 0, 0.9))
            .toList();

        assertThat(chain.validate(new PoseFrame(points, "squat", 1280, 720, "s3")).reason()).isEqualTo("too_many_points");
    }

    @Test
    void frameWithoutSessionIsRejected() {
        assertThat(chain.validate(frame(null, 0.9, 1280, 720)).reason()).isEqualTo("missing_session");
    }

    @Test
    void forgetResetsRateLimitForSession() {
        chain.validate(frame("s4", 0.9, 1280, 720));
        chain.forget("s4");

        assertThat(chain.validate(frame("s4", 0.9, 1280, 720)).accepted()).isTrue();
    }

    private static PoseFrame frame(String sessionId, double confidence, int width, int height) {
        return new PoseFrame(
            List.of(new PosePoint("left_hip", 0.5, 0.5, confidence)),
            "squat", width, height, sessionId);
    }
}
