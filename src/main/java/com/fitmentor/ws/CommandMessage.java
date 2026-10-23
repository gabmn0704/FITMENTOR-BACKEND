package com.fitmentor.ws;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Body of a STOMP SEND to {@code /app/command}. */
public record CommandMessage(@JsonProperty("session_id") String sessionId, String name) {
}
