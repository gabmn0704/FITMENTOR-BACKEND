package com.fitmentor.ws;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decorator: skips a message identical to the last one sent to the same destination.
 * Frames at 150 ms intervals often produce the same coaching text; this keeps the socket
 * quiet while any change (score, reps, state) is still delivered immediately.
 */
public final class DuplicateSuppressingFeedbackChannel extends FeedbackChannelDecorator {

    private final Map<String, Object> lastSent = new ConcurrentHashMap<>();

    public DuplicateSuppressingFeedbackChannel(FeedbackChannel delegate) {
        super(delegate);
    }

    @Override
    public void send(String destination, Object payload) {
        Object previous = lastSent.put(destination, payload);
        if (previous != null && Objects.equals(previous, payload)) {
            return;
        }
        delegate.send(destination, payload);
    }
}
