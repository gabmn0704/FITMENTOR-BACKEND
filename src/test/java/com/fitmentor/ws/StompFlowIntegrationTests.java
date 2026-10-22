package com.fitmentor.ws;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** End-to-end STOMP test over a real WebSocket connection to the embedded server. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StompFlowIntegrationTests {

    @LocalServerPort
    private int port;

    @Test
    void poseAndCommandRoundTripOnPerSessionTopics() throws Exception {
        WebSocketStompClient client = client();
        StompSession session = client.connectAsync(url(), new WebSocketHttpHeaders(), new StompHeaders(),
            new StompSessionHandlerAdapter() { }).get(10, TimeUnit.SECONDS);
        BlockingQueue<Map<String, Object>> feedback = new LinkedBlockingQueue<>();
        BlockingQueue<Map<String, Object>> states = new LinkedBlockingQueue<>();
        session.subscribe("/topic/feedback.it-1", handler(feedback));
        session.subscribe("/topic/state.it-1", handler(states));

        session.send("/app/command", Map.of("session_id", "it-1", "name", "skip"));
        Map<String, Object> state = states.poll(10, TimeUnit.SECONDS);
        assertThat(state).isNotNull();
        assertThat(state).containsEntry("type", "state").containsEntry("state", "ACTIVE_SET");

        session.send("/app/pose", Map.of(
            "session_id", "it-1",
            "exercise", "squat",
            "frame_width", 1280,
            "frame_height", 720,
            "points", List.of(
                Map.of("name", "left_shoulder", "x", 0.0, "y", -1.0, "confidence", 0.9),
                Map.of("name", "left_hip", "x", 0.0, "y", 0.0, "confidence", 0.9),
                Map.of("name", "left_knee", "x", 1.0, "y", 0.0, "confidence", 0.9),
                Map.of("name", "left_ankle", "x", 1.0, "y", 1.0, "confidence", 0.9))));
        Map<String, Object> message = feedback.poll(10, TimeUnit.SECONDS);
        assertThat(message).isNotNull();
        assertThat(message).containsEntry("type", "feedback").containsEntry("exercise", "squat");
        assertThat(message).containsKeys("score", "message", "feedback", "phase", "reps", "state");
        session.disconnect();
    }

    @Test
    void connectWithInvalidTokenIsRejected() {
        WebSocketStompClient client = client();
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer not-a-token");

        assertThatThrownBy(() -> client.connectAsync(url(), new WebSocketHttpHeaders(), headers,
                new StompSessionHandlerAdapter() { }).get(10, TimeUnit.SECONDS))
            .isInstanceOf(ExecutionException.class);
    }

    private WebSocketStompClient client() {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new MappingJackson2MessageConverter());
        return client;
    }

    private String url() {
        return "ws://localhost:" + port + "/ws";
    }

    private static StompFrameHandler handler(BlockingQueue<Map<String, Object>> queue) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add((Map<String, Object>) payload);
            }
        };
    }
}
