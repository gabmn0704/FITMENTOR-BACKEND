package com.fitmentor.ws;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end: STOMP client, this backend and a running FITMENTOR-IA. Synthetic squat frames
 * must come back with the score, phase and repetition count computed by the AI service.
 * Enabled only when {@code FITMENTOR_TEST_AI_URL} points to the AI service (e.g. http://127.0.0.1:8000).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "FITMENTOR_TEST_AI_URL", matches = "https?://.+")
class AiServiceStompIntegrationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @DynamicPropertySource
    static void aiService(DynamicPropertyRegistry registry) {
        registry.add("ai.service-url", () -> System.getenv("FITMENTOR_TEST_AI_URL"));
    }

    @Test
    void squatFramesAreScoredAndCountedByTheAiService() throws Exception {
        String sessionId = "e2e-" + UUID.randomUUID();
        StompSession session = StompClientSupport.connect(port, StompClientSupport.guestToken(rest));
        BlockingQueue<Map<String, Object>> feedback = StompClientSupport.subscribe(session, "/topic/feedback." + sessionId);
        Thread.sleep(200);

        StompClientSupport.replay(session, sessionId, SquatFrames.oneRepetition());
        Thread.sleep(500);

        List<Map<String, Object>> received = new ArrayList<>();
        feedback.drainTo(received);
        assertThat(received).isNotEmpty();
        assertThat(received).extracting(message -> message.get("phase")).contains("up", "bottom");
        assertThat(received).allSatisfy(message -> assertThat(((Number) message.get("score")).doubleValue())
            .isBetween(0.0, 100.0));
        Map<String, Object> last = received.get(received.size() - 1);
        assertThat(last).containsEntry("type", "feedback").containsEntry("exercise", "squat");
        assertThat(((Number) last.get("reps")).intValue()).isEqualTo(1);
        assertThat((String) last.get("message")).isNotBlank();
        session.disconnect();
    }
}
