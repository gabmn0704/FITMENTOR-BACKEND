package com.fitmentor.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Keeps the AI service awake while this backend is awake. Free hosting suspends idle services and the first
 * question after a nap would otherwise fall back to canned answers; a ping every nine minutes (and one shortly
 * after start-up) avoids it. The interval is {@code ai.keep-warm-ms}.
 */
@Component
public class AiKeepWarm {

    private final AiServiceClient ai;

    @Autowired
    public AiKeepWarm(AiServiceClient ai) {
        this.ai = ai;
    }

    @Scheduled(initialDelayString = "${ai.keep-warm-initial-ms:8000}", fixedDelayString = "${ai.keep-warm-ms:540000}")
    public void ping() {
        ai.warmUp();
    }
}
