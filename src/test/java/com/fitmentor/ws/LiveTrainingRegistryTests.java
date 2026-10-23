package com.fitmentor.ws;

import com.fitmentor.domain.pose.PoseFrame;
import com.fitmentor.domain.pose.PosePoint;
import com.fitmentor.domain.pose.validation.FrameValidationChain;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class LiveTrainingRegistryTests {

    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-10-21T12:00:00Z"));
    private final Clock clock = new Clock() {
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now.get(); }
    };
    private final FrameValidationChain chain = new FrameValidationChain(clock);
    private final LiveTrainingRegistry registry = new LiveTrainingRegistry(event -> { }, chain, clock);

    @Test
    void idleSessionsAndTheirRateLimitStateExpire() {
        registry.getOrCreate("old", null);
        chain.validate(frame("old"));
        now.set(now.get().plus(LiveTrainingRegistry.IDLE_TTL).plusSeconds(1));
        registry.getOrCreate("fresh", null);

        registry.expireIdleSessions();

        assertThat(registry.find("old")).isEmpty();
        assertThat(registry.find("fresh")).isPresent();
        // The rate-limit entry of "old" is gone: a new frame is accepted immediately.
        assertThat(chain.validate(frame("old")).accepted()).isTrue();
    }

    @Test
    void registryRefusesNewSessionsWhenFullButKeepsExistingOnes() {
        for (int i = 0; i < LiveTrainingRegistry.MAX_SESSIONS; i++) {
            assertThat(registry.getOrCreate("s" + i, null)).isPresent();
        }

        assertThat(registry.getOrCreate("one-too-many", null)).isEmpty();
        assertThat(registry.getOrCreate("s0", null)).isPresent();
        assertThat(registry.size()).isEqualTo(LiveTrainingRegistry.MAX_SESSIONS);
    }

    @Test
    void sessionIdsAreRestrictedToSafeCharacters() {
        assertThat(LiveTrainingRegistry.isValidSessionId(UUID.randomUUID().toString())).isTrue();
        assertThat(LiveTrainingRegistry.isValidSessionId("a*")).isFalse();
        assertThat(LiveTrainingRegistry.isValidSessionId("a.b")).isFalse();
        assertThat(LiveTrainingRegistry.isValidSessionId("x".repeat(101))).isFalse();
        assertThat(LiveTrainingRegistry.isValidSessionId(null)).isFalse();
    }

    @Test
    void duplicateSuppressionMemoryIsBounded() {
        AtomicInteger delivered = new AtomicInteger();
        DuplicateSuppressingFeedbackChannel channel =
            new DuplicateSuppressingFeedbackChannel((destination, payload) -> delivered.incrementAndGet());

        for (int i = 0; i < DuplicateSuppressingFeedbackChannel.MAX_DESTINATIONS + 500; i++) {
            channel.send("/topic/feedback.s" + i, "same");
        }
        channel.send("/topic/feedback.s1", "changed");
        channel.send("/topic/feedback.s1", "changed");

        assertThat(channel.trackedDestinations()).isEqualTo(DuplicateSuppressingFeedbackChannel.MAX_DESTINATIONS);
        assertThat(delivered.get()).isEqualTo(DuplicateSuppressingFeedbackChannel.MAX_DESTINATIONS + 501);
    }

    private static PoseFrame frame(String sessionId) {
        return new PoseFrame(List.of(new PosePoint("left_hip", 0.5, 0.5, 0.9)), "squat", 640, 480, sessionId);
    }
}
