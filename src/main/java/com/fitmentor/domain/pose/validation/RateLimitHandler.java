package com.fitmentor.domain.pose.validation;

import com.fitmentor.domain.pose.PoseFrame;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Second link: drops frames that arrive less than 150 ms after the previous accepted frame
 * of the same session. The timestamps live in a concurrent map keyed by session id.
 */
public final class RateLimitHandler extends FrameHandler {

    static final Duration MIN_INTERVAL = Duration.ofMillis(150);

    private final Clock clock;
    private final Map<String, Instant> lastAccepted = new ConcurrentHashMap<>();

    public RateLimitHandler(Clock clock) {
        this.clock = clock;
    }

    @Override
    protected FrameVerdict check(PoseFrame frame) {
        String sessionId = frame.sessionId();
        if (sessionId == null || sessionId.isBlank()) {
            return FrameVerdict.rejected("missing_session");
        }
        Instant now = clock.instant();
        AtomicBoolean allowed = new AtomicBoolean(false);
        lastAccepted.compute(sessionId, (key, previous) -> {
            if (previous == null || !now.isBefore(previous.plus(MIN_INTERVAL))) {
                allowed.set(true);
                return now;
            }
            return previous;
        });
        return allowed.get() ? FrameVerdict.ACCEPTED : FrameVerdict.rejected("rate_limited");
    }

    /** Forgets a session, e.g. when it expires. */
    public void forget(String sessionId) {
        lastAccepted.remove(sessionId);
    }

    /** Removes the entries whose last accepted frame is older than {@code cutoff}. */
    public void forgetIdleSince(Instant cutoff) {
        lastAccepted.values().removeIf(last -> last.isBefore(cutoff));
    }

    int trackedSessions() {
        return lastAccepted.size();
    }
}
