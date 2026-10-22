package com.fitmentor.ws;

import org.springframework.messaging.simp.SimpMessagingTemplate;

/** Concrete component of the Decorator pattern: publishes to the STOMP broker. */
public final class StompFeedbackChannel implements FeedbackChannel {

    private final SimpMessagingTemplate template;

    public StompFeedbackChannel(SimpMessagingTemplate template) {
        this.template = template;
    }

    @Override
    public void send(String destination, Object payload) {
        template.convertAndSend(destination, payload);
    }
}
