package com.fitmentor.domain.feedback;

/** Observer pattern: receives every event published by a session subject. */
public interface FeedbackObserver {
    void update(SessionEvent event);
}
