package com.fitmentor.domain.feedback;

/** Something that happened in one live training session; observers decide how to deliver it. */
public sealed interface SessionEvent permits FeedbackEvent, StateEvent {

    String sessionId();
}
