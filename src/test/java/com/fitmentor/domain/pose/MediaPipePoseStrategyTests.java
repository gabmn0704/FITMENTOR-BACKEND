package com.fitmentor.domain.pose;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class MediaPipePoseStrategyTests {
    @Test
    void postsPoseFrameAndCombinesAiCorrections() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> requestBody = new AtomicReference<>();
        server.createContext("/analyze", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"score\":82.5,\"feedback\":[\"Lower the hips.\",\"Keep your chest stable.\"],\"analyzed\":true}"
                .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            MediaPipePoseStrategy strategy = new MediaPipePoseStrategy(
                "http://127.0.0.1:" + server.getAddress().getPort()
            );
            PoseFrame frame = new PoseFrame(
                List.of(new PosePoint("left_hip", 0.5, 0.4, 0.92)),
                "squat"
            );

            PoseAnalysis result = strategy.analyze(frame);

            assertThat(requestBody.get())
                .contains("\"exercise\":\"squat\"")
                .contains("\"name\":\"left_hip\"");
            assertThat(result.score()).isEqualTo(82.5);
            assertThat(result.feedback()).isEqualTo("Lower the hips. Keep your chest stable.");
        } finally {
            server.stop(0);
        }
    }
}