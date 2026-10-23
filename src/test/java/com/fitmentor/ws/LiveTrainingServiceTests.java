package com.fitmentor.ws;

import com.fitmentor.domain.pose.MediaPipePoseStrategy;
import com.fitmentor.domain.pose.PoseAnalyzer;
import com.fitmentor.domain.pose.PoseFrame;
import com.fitmentor.domain.pose.PosePoint;
import com.fitmentor.domain.pose.validation.FrameValidationChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class LiveTrainingServiceTests {

    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-10-21T12:00:00Z"));
    private final Clock clock = new Clock() {
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now.get(); }
    };
    private final List<Sent> sent = new CopyOnWriteArrayList<>();
    private LiveTrainingService service;

    @BeforeEach
    void setUp() {
        FeedbackChannel channel = (destination, payload) -> sent.add(new Sent(destination, payload));
        FrameValidationChain chain = new FrameValidationChain(clock);
        LiveTrainingRegistry registry = new LiveTrainingRegistry(
            new StompFeedbackObserver(channel), chain, clock);
        service = new LiveTrainingService(chain, new PoseAnalyzer(List.of(new MediaPipePoseStrategy())), registry);
    }

    @Test
    void poseFrameIsSentOnlyToItsSessionTopic() {
        service.handlePose(squatFrame("s1"), null);

        assertThat(sent).hasSize(1);
        assertThat(sent.get(0).destination()).isEqualTo("/topic/feedback.s1");
        assertThat(sent.get(0).payload()).isInstanceOfSatisfying(FeedbackPayloads.Feedback.class, feedback -> {
            assertThat(feedback.type()).isEqualTo("feedback");
            assertThat(feedback.reps()).isZero();
            assertThat(feedback.state()).isEqualTo("WARMUP");
            assertThat(feedback.exercise()).isEqualTo("squat");
            assertThat(feedback.score()).isEqualTo(100.0);
        });
    }

    @Test
    void commandsPublishStateAndUndoRestoresIt() {
        service.handleCommand(new CommandMessage("s2", "skip"), null);
        service.handleCommand(new CommandMessage("s2", "undo"), null);

        assertThat(sent).extracting(Sent::destination).containsOnly("/topic/state.s2");
        assertThat(sent).extracting(sentItem -> ((FeedbackPayloads.State) sentItem.payload()).state())
            .containsExactly("ACTIVE_SET", "WARMUP");
    }

    @Test
    void rateLimitedFramesAreIgnoredUntilIntervalPasses() {
        service.handlePose(squatFrame("s3"), null);
        now.set(now.get().plusMillis(100));
        service.handlePose(squatFrame("s3"), null);
        now.set(now.get().plusMillis(60));
        service.handlePose(squatFrame("s3"), null);

        assertThat(sent).hasSize(2);
    }

    @Test
    void lowConfidenceFramesProduceNoMessage() {
        service.handlePose(new PoseFrame(List.of(new PosePoint("left_hip", 0, 0, 0.05)), "squat", 1280, 720, "s4"), null);

        assertThat(sent).isEmpty();
    }

    @Test
    void sessionOwnedByAnotherUserIgnoresFrames() {
        UUID owner = UUID.randomUUID();
        service.handlePose(squatFrame("s5"), owner);
        sent.clear();
        now.set(now.get().plusMillis(200));

        service.handlePose(squatFrame("s5"), UUID.randomUUID());
        service.handleCommand(new CommandMessage("s5", "pause"), UUID.randomUUID());

        assertThat(sent).isEmpty();
    }

    @Test
    void malformedCommandsAreIgnored() {
        service.handleCommand(new CommandMessage(null, "pause"), null);
        service.handleCommand(new CommandMessage("s6", "explode"), null);

        assertThat(sent).isEmpty();
    }

    private static PoseFrame squatFrame(String sessionId) {
        // Hip at origin, knee to the right, ankle below it: knee angle 90 degrees.
        return new PoseFrame(List.of(
            new PosePoint("left_shoulder", 0, -1, 0.9),
            new PosePoint("left_hip", 0, 0, 0.9),
            new PosePoint("left_knee", 1, 0, 0.9),
            new PosePoint("left_ankle", 1, 1, 0.9)),
            "squat", 1280, 720, sessionId);
    }

    private record Sent(String destination, Object payload) {
    }
}
