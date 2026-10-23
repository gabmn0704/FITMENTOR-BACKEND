package com.fitmentor.ws;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.StompSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * With the AI service down (nothing listens on port 9) the socket keeps answering: frames are
 * scored by the local strategy, without phase and without counting repetitions.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "ai.service-url=http://127.0.0.1:9")
class AiFallbackStompIntegrationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Test
    void framesAreScoredLocallyWhenTheAiServiceIsDown() throws Exception {
        StompSession session = StompClientSupport.connect(port, StompClientSupport.guestToken(rest));
        BlockingQueue<Map<String, Object>> feedback = StompClientSupport.subscribe(session, "/topic/feedback.fallback-1");
        Thread.sleep(200);

        StompClientSupport.replay(session, "fallback-1", List.of(175.0, 120.0, 90.0, 175.0));
        Thread.sleep(500);

        List<Map<String, Object>> received = new ArrayList<>();
        feedback.drainTo(received);
        assertThat(received).isNotEmpty();
        assertThat(received).allSatisfy(message -> {
            assertThat(message).containsEntry("type", "feedback").containsEntry("reps", 0);
            assertThat(message.get("phase")).isNull();
        });
        assertThat(received).anySatisfy(message ->
            assertThat(((Number) message.get("score")).doubleValue()).isGreaterThan(0.0));
        session.disconnect();
    }
}
