package com.fitmentor.ws;

/**
 * Outbound channel for session messages. Decorators (logging, duplicate suppression) wrap
 * the STOMP implementation without the observers knowing about them.
 */
public interface FeedbackChannel {

    void send(String destination, Object payload);
}
