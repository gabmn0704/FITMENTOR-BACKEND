package com.fitmentor.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.web.client.TestRestTemplate;
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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Helpers shared by the STOMP end-to-end tests: guest login, authenticated connect, frame replay. */
final class StompClientSupport {

    private StompClientSupport() {
    }

    static String guestToken(TestRestTemplate rest) throws Exception {
        String body = rest.postForObject("/api/auth/guest", Map.of("name", "STOMP"), String.class);
        return new ObjectMapper().readTree(body).get("token").asText();
    }

    static StompSession connect(int port, String token) throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new MappingJackson2MessageConverter());
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + token);
        return client.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(), headers,
            new StompSessionHandlerAdapter() { }).get(10, TimeUnit.SECONDS);
    }

    static BlockingQueue<Map<String, Object>> subscribe(StompSession session, String destination) {
        BlockingQueue<Map<String, Object>> queue = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add((Map<String, Object>) payload);
            }
        });
        return queue;
    }

    /** Sends the frames 170 ms apart (above the 150 ms rate limit of the validation chain). */
    static void replay(StompSession session, String sessionId, List<Double> kneeAngles) throws InterruptedException {
        for (double angle : kneeAngles) {
            session.send("/app/pose", SquatFrames.frame(sessionId, angle));
            Thread.sleep(170);
        }
    }
}
