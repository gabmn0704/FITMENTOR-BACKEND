package com.fitmentor.ws;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Decorator: skips a message identical to the last one sent to the same destination.
 * Frames at 150 ms intervals often produce the same coaching text; this keeps the socket
 * quiet while any change (score, reps, state) is still delivered immediately.
 *
 * <p>The last payload per destination is kept in an access-ordered LRU map bounded to
 * {@link #MAX_DESTINATIONS} entries, so destinations of finished sessions do not accumulate.
 */
public final class DuplicateSuppressingFeedbackChannel extends FeedbackChannelDecorator {

    static final int MAX_DESTINATIONS = 20_000;

    private final Map<String, Object> lastSent = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Object> eldest) {
            return size() > MAX_DESTINATIONS;
        }
    };

    public DuplicateSuppressingFeedbackChannel(FeedbackChannel delegate) {
        super(delegate);
    }

    @Override
    public void send(String destination, Object payload) {
        Object previous;
        synchronized (lastSent) {
            previous = lastSent.put(destination, payload);
        }
        if (previous != null && Objects.equals(previous, payload)) {
            return;
        }
        delegate.send(destination, payload);
    }

    int trackedDestinations() {
        synchronized (lastSent) {
            return lastSent.size();
        }
    }
}
