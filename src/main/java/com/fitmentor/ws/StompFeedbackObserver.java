package com.fitmentor.ws;

import com.fitmentor.domain.feedback.FeedbackEvent;
import com.fitmentor.domain.feedback.FeedbackObserver;
import com.fitmentor.domain.feedback.SessionEvent;
import com.fitmentor.domain.feedback.StateEvent;

/**
 * Observer: turns session events into STOMP messages on the per-session topics. Messages
 * are only delivered to the subscribers of that session id, never broadcast.
 */
public final class StompFeedbackObserver implements FeedbackObserver {

    private final FeedbackChannel channel;

    public StompFeedbackObserver(FeedbackChannel channel) {
        this.channel = channel;
    }

    @Override
    public void update(SessionEvent event) {
        if (event instanceof FeedbackEvent feedback) {
            channel.send("/topic/feedback." + feedback.sessionId(), new FeedbackPayloads.Feedback(
                "feedback", feedback.score(), feedback.message(), feedback.feedback(),
                feedback.phase(), feedback.reps(), feedback.state(), feedback.exercise()));
        } else if (event instanceof StateEvent state) {
            channel.send("/topic/state." + state.sessionId(),
                new FeedbackPayloads.State("state", state.state(), state.exercise()));
        }
    }
}
