package com.fitmentor.domain.feedback;

/** The session state changed (after a command or an undo). */
public record StateEvent(String sessionId, String state, String exercise) implements SessionEvent {
}
