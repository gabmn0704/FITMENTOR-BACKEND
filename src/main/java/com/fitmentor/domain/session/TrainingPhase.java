package com.fitmentor.domain.session;

/** Names of the session states as sent to the frontend in {@code /topic/state}. */
public enum TrainingPhase { WARMUP, ACTIVE_SET, REST, PAUSED, FINISHED }
