package com.fitmentor.ws;

/**
 * Decorator pattern base: adds one behaviour to a {@link FeedbackChannel} and delegates
 * the actual send to the wrapped channel.
 */
public abstract class FeedbackChannelDecorator implements FeedbackChannel {

    protected final FeedbackChannel delegate;

    protected FeedbackChannelDecorator(FeedbackChannel delegate) {
        this.delegate = delegate;
    }
}
