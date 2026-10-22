package com.fitmentor.ws;

import com.fitmentor.domain.feedback.FeedbackObserver;
import com.fitmentor.domain.session.LiveTraining;
import com.fitmentor.domain.pose.validation.FrameValidationChain;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton (one Spring bean) holding the live sessions in a concurrent map. Sessions idle
 * for more than 30 minutes are removed by a scheduled sweep, together with their rate-limit state.
 */
@Component
public class LiveTrainingRegistry {

    static final Duration IDLE_TTL = Duration.ofMinutes(30);

    private final Map<String, LiveTraining> sessions = new ConcurrentHashMap<>();
    private final FeedbackObserver observer;
    private final FrameValidationChain frameChain;
    private final Clock clock;

    public LiveTrainingRegistry(FeedbackObserver observer, FrameValidationChain frameChain, Clock clock) {
        this.observer = observer;
        this.frameChain = frameChain;
        this.clock = clock;
    }

    /** Returns the session, creating it on first use, and refreshes its activity time. */
    public LiveTraining getOrCreate(String sessionId, UUID owner) {
        LiveTraining live = sessions.computeIfAbsent(sessionId, id -> {
            LiveTraining created = new LiveTraining(id, owner, clock.instant());
            created.subscribe(observer);
            return created;
        });
        live.touch(clock.instant());
        return live;
    }

    public int size() {
        return sessions.size();
    }

    /** Removes sessions idle for longer than {@link #IDLE_TTL}. */
    @Scheduled(fixedDelayString = "${fitmentor.ws.sweep-interval-ms:60000}")
    public void expireIdleSessions() {
        Instant cutoff = clock.instant().minus(IDLE_TTL);
        sessions.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().lastActivity().isBefore(cutoff);
            if (expired) {
                frameChain.forget(entry.getKey());
            }
            return expired;
        });
    }
}
