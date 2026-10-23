package com.fitmentor.ws;

import com.fitmentor.domain.command.TrainingCommands;
import com.fitmentor.domain.feedback.SessionEvent;
import com.fitmentor.domain.feedback.StateEvent;
import com.fitmentor.domain.pose.PoseAnalysis;
import com.fitmentor.domain.session.LiveTraining;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Observer (live session notifies every subscriber) and Decorator (stacked outbound channel). */
class ObserverAndDecoratorTests {

    @Test
    void liveSessionNotifiesEverySubscribedObserver() {
        LiveTraining live = new LiveTraining("obs-1", null, Instant.EPOCH);
        List<SessionEvent> first = new ArrayList<>();
        List<SessionEvent> second = new ArrayList<>();
        live.subscribe(first::add);
        live.subscribe(second::add);

        live.apply(TrainingCommands.byName("skip").orElseThrow());
        live.recordAnalysis(new PoseAnalysis(80, List.of("Bien."), "down", 2, "ai-service"), "squat");

        assertThat(first).hasSize(2).isEqualTo(second);
        assertThat(first.get(0)).isEqualTo(new StateEvent("obs-1", "ACTIVE_SET", null));
    }

    @Test
    void decoratorsWrapTheStompChannelWithoutObserversKnowing() {
        List<String> delivered = new ArrayList<>();
        FeedbackChannel stomp = (destination, payload) -> delivered.add(destination + "=" + payload);
        FeedbackChannel stack = new DuplicateSuppressingFeedbackChannel(new LoggingFeedbackChannel(stomp));
        StompFeedbackObserver observer = new StompFeedbackObserver(stack);

        observer.update(new StateEvent("dec-1", "WARMUP", "squat"));
        observer.update(new StateEvent("dec-1", "WARMUP", "squat"));
        observer.update(new StateEvent("dec-1", "REST", "squat"));
        observer.update(new StateEvent("dec-2", "WARMUP", "squat"));

        assertThat(delivered).containsExactly(
            "/topic/state.dec-1=State[type=state, state=WARMUP, exercise=squat]",
            "/topic/state.dec-1=State[type=state, state=REST, exercise=squat]",
            "/topic/state.dec-2=State[type=state, state=WARMUP, exercise=squat]");
    }
}
