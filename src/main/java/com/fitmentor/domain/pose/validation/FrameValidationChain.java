package com.fitmentor.domain.pose.validation;

import com.fitmentor.domain.pose.PoseFrame;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * Builds the Chain of Responsibility once and exposes it as a singleton Spring bean, so
 * every WebSocket session shares the same rate-limit state.
 * Order: confidence, then rate limit, then size.
 */
@Component
public class FrameValidationChain {

    private final RateLimitHandler rateLimit;
    private final FrameHandler head;

    public FrameValidationChain(Clock clock) {
        this.rateLimit = new RateLimitHandler(clock);
        FrameHandler confidence = new ConfidenceHandler();
        confidence.setNext(rateLimit).setNext(new SizeHandler());
        this.head = confidence;
    }

    public FrameVerdict validate(PoseFrame frame) {
        return head.handle(frame);
    }

    public void forget(String sessionId) {
        rateLimit.forget(sessionId);
    }

    /** Drops rate-limit entries of sessions without accepted frames since {@code cutoff}. */
    public void forgetIdleSince(Instant cutoff) {
        rateLimit.forgetIdleSince(cutoff);
    }
}
