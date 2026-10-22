package com.fitmentor.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decorator: debug-logs the destination of each message. The payload is never logged, so
 * no pose data or user content reaches the logs.
 */
public final class LoggingFeedbackChannel extends FeedbackChannelDecorator {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFeedbackChannel.class);

    public LoggingFeedbackChannel(FeedbackChannel delegate) {
        super(delegate);
    }

    @Override
    public void send(String destination, Object payload) {
        logger.debug("Sending session message to {}", destination);
        delegate.send(destination, payload);
    }
}
