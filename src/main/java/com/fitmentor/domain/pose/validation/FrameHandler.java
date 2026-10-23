package com.fitmentor.domain.pose.validation;

import com.fitmentor.domain.pose.PoseFrame;

/**
 * Chain of Responsibility: each handler checks one property of a frame and either rejects
 * it or passes it to the next handler. The v2 order is confidence, rate limit, size.
 */
public abstract class FrameHandler {

    private FrameHandler next;

    /** Links the next handler and returns it, so chains can be built fluently. */
    public FrameHandler setNext(FrameHandler next) {
        this.next = next;
        return next;
    }

    public final FrameVerdict handle(PoseFrame frame) {
        FrameVerdict verdict = check(frame);
        if (!verdict.accepted()) {
            return verdict;
        }
        return next == null ? FrameVerdict.ACCEPTED : next.handle(frame);
    }

    /** This handler's own check. */
    protected abstract FrameVerdict check(PoseFrame frame);
}
