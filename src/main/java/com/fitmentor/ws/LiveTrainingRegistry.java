package com.fitmentor.ws;

import com.fitmentor.domain.feedback.FeedbackObserver;
import com.fitmentor.domain.pose.validation.FrameValidationChain;
import com.fitmentor.domain.session.LiveTraining;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Holds the live sessions in a {@link ConcurrentHashMap}. It is a Spring singleton bean (one
 * instance per application context, managed by the container, not a hand-written GoF Singleton).
 *
 * <p>Memory is bounded: sessions idle for more than 30 minutes are removed by a scheduled sweep
 * together with their rate-limit state, and at most {@link #MAX_SESSIONS} sessions live at once.
 * Session ids must be 1-100 characters of {@code [A-Za-z0-9_-]} (the frontend uses UUIDs).
 */
@Component
public class LiveTrainingRegistry {

    static final Duration IDLE_TTL = Duration.ofMinutes(30);
    static final int MAX_SESSIONS = 10_000;
    private static final Pattern SESSION_ID = Pattern.compile("^[A-Za-z0-9_-]{1,100}$");

    private final Map<String, LiveTraining> sessions = new ConcurrentHashMap<>();
    private final FeedbackObserver observer;
    private final FrameValidationChain frameChain;
    private final Clock clock;

    public LiveTrainingRegistry(FeedbackObserver observer, FrameValidationChain frameChain, Clock clock) {
        this.observer = observer;
        this.frameChain = frameChain;
        this.clock = clock;
    }

    public static boolean isValidSessionId(String sessionId) {
        return sessionId != null && SESSION_ID.matcher(sessionId).matches();
    }

    /**
     * Returns the session, creating it on first use, and refreshes its activity time.
     * Empty when the registry is full and the session does not exist yet.
     */
    public Optional<LiveTraining> getOrCreate(String sessionId, UUID owner) {
        LiveTraining existing = sessions.get(sessionId);
        if (existing == null && sessions.size() >= MAX_SESSIONS) {
            return Optional.empty();
        }
        LiveTraining live = existing != null ? existing : sessions.computeIfAbsent(sessionId, id -> {
            LiveTraining created = new LiveTraining(id, owner, clock.instant());
            created.subscribe(observer);
            return created;
        });
        live.touch(clock.instant());
        return Optional.of(live);
    }

    /** The live session with this id, if any (used to authorize topic subscriptions). */
    public Optional<LiveTraining> find(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public int size() {
        return sessions.size();
    }

    /** Removes sessions idle for longer than {@link #IDLE_TTL} and stale rate-limit entries. */
    @Scheduled(fixedDelayString = "${fitmentor.ws.sweep-interval-ms:60000}")
    public void expireIdleSessions() {
        Instant cutoff = clock.instant().minus(IDLE_TTL);
        sessions.entrySet().removeIf(entry -> entry.getValue().lastActivity().isBefore(cutoff));
        frameChain.forgetIdleSince(cutoff);
    }
}
