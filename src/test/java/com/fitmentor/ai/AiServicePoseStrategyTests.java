package com.fitmentor.ai;

import com.fitmentor.domain.pose.MediaPipePoseStrategy;
import com.fitmentor.domain.pose.PoseAnalysis;
import com.fitmentor.domain.pose.PoseFrame;
import com.fitmentor.domain.pose.PosePoint;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AiServicePoseStrategyTests {

    @Test
    void sendsFrameToAiAndUsesRepsAndPhaseFromIt() throws IOException {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = server("/analyze", exchange -> {
            requestBody.set(read(exchange));
            respond(exchange, "{\"score\":82.5,\"feedback\":[\"Lower the hips.\",\"Keep your chest stable.\"],"
                + "\"analyzed\":true,\"phase\":\"down\",\"reps\":4,\"message\":\"ok\"}");
        });
        try {
            AiServicePoseStrategy strategy = strategy(server.getAddress().getPort());

            PoseAnalysis result = strategy.analyze(frame());

            assertThat(requestBody.get())
                .contains("\"exercise\":\"squat\"")
                .contains("\"session_id\":\"session-7\"")
                .contains("\"frame_width\":1280")
                .contains("\"name\":\"left_hip\"");
            assertThat(result.score()).isEqualTo(82.5);
            assertThat(result.feedback()).containsExactly("Lower the hips.", "Keep your chest stable.");
            assertThat(result.reps()).isEqualTo(4);
            assertThat(result.phase()).isEqualTo("down");
            assertThat(result.engine()).isEqualTo("ai-service");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fallsBackToLocalAnalysisWithoutRepsWhenAiIsDown() throws IOException {
        HttpServer server = server("/analyze", exchange -> respond(exchange, "{}"));
        int port = server.getAddress().getPort();
        server.stop(0);

        PoseAnalysis result = strategy(port).analyze(frame());

        assertThat(result.engine()).isEqualTo("local");
        assertThat(result.reps()).isNull();
    }

    @Test
    void fallsBackWhenAiReportsNotAnalyzed() throws IOException {
        HttpServer server = server("/analyze", exchange -> respond(exchange,
            "{\"score\":0,\"feedback\":[],\"analyzed\":false,\"message\":\"no person\"}"));
        try {
            PoseAnalysis result = strategy(server.getAddress().getPort()).analyze(frame());

            assertThat(result.engine()).isEqualTo("local");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void validationErrorFallsBackLocallyWithoutPausingTheService() throws IOException {
        AtomicInteger calls = new AtomicInteger();
        HttpServer server = server("/analyze", exchange -> {
            calls.incrementAndGet();
            respond(exchange, 422, "{\"detail\":[{\"type\":\"too_long\"}]}");
        });
        try {
            AiServicePoseStrategy strategy = strategy(server.getAddress().getPort());

            assertThat(strategy.analyze(frame()).engine()).isEqualTo("local");
            assertThat(strategy.analyze(frame()).engine()).isEqualTo("local");
            assertThat(calls.get()).isEqualTo(2);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void serverErrorPausesCallsDuringTheCoolDown() throws IOException {
        AtomicInteger calls = new AtomicInteger();
        HttpServer server = server("/analyze", exchange -> {
            calls.incrementAndGet();
            respond(exchange, 503, "{}");
        });
        try {
            AiServiceClient client = new AiServiceClient(RestClient.builder(),
                "http://127.0.0.1:" + server.getAddress().getPort(), Duration.ofMinutes(1), AiServiceClient.COACH_TIMEOUT);

            assertThat(client.analyze(frame())).isEmpty();
            assertThat(client.analyze(frame())).isEmpty();
            assertThat(calls.get()).isEqualTo(1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void coachSendsStructuredContextAndReturnsSource() throws IOException {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = server("/coach", exchange -> {
            requestBody.set(read(exchange));
            respond(exchange, "{\"answer\":\"Descansa 60 segundos.\",\"source\":\"claude\"}");
        });
        int port = server.getAddress().getPort();
        try {
            Optional<AiServiceClient.CoachReply> reply = client(port).coach("¿Cuánto descanso?",
                new AiServiceClient.CoachContext("squat", 80.0, null, "BEGINNER"));

            assertThat(reply).contains(new AiServiceClient.CoachReply("Descansa 60 segundos.", "claude"));
            assertThat(requestBody.get())
                .contains("\"context\":{\"exercise\":\"squat\",\"score\":80.0,\"level\":\"BEGINNER\"}")
                .doesNotContain("reps");
        } finally {
            server.stop(0);
        }
        assertThat(client(port).coach("hola", null)).isEmpty();
    }

    @Test
    void slowCoachAnswerWithinItsTimeoutIsUsed() throws IOException {
        // The AI service may take several seconds to get Claude's answer; the coach timeout must allow it.
        HttpServer server = server("/coach", exchange -> {
            sleep(3000);
            respond(exchange, "{\"answer\":\"Descansa 60 segundos.\",\"source\":\"claude\"}");
        });
        try {
            AiServiceClient client = new AiServiceClient(RestClient.builder(),
                "http://127.0.0.1:" + server.getAddress().getPort(), Duration.ofMinutes(1), AiServiceClient.COACH_TIMEOUT);

            assertThat(client.coach("¿Cuánto descanso?", null))
                .contains(new AiServiceClient.CoachReply("Descansa 60 segundos.", "claude"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void analyzeKeepsItsShortTimeoutWhileCoachHasALongerOne() throws IOException {
        HttpServer server = server("/analyze", exchange -> {
            sleep(3000);
            respond(exchange, "{\"score\":80,\"feedback\":[],\"analyzed\":true,\"phase\":\"down\",\"reps\":1,\"message\":\"ok\"}");
        });
        try {
            long started = System.nanoTime();
            Optional<AiPoseResponse> reply = client(server.getAddress().getPort()).analyze(frame());

            assertThat(reply).isEmpty();
            assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofMillis(2500));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void coachTimeoutDoesNotPauseAnalyze() throws IOException {
        AtomicInteger coachCalls = new AtomicInteger();
        AtomicInteger analyzeCalls = new AtomicInteger();
        HttpServer server = server("/coach", exchange -> {
            coachCalls.incrementAndGet();
            sleep(1000);
            respond(exchange, "{\"answer\":\"tarde\",\"source\":\"claude\"}");
        });
        server.createContext("/analyze", exchange -> {
            analyzeCalls.incrementAndGet();
            respond(exchange, "{\"score\":80,\"feedback\":[],\"analyzed\":true,\"phase\":\"down\",\"reps\":1,\"message\":\"ok\"}");
        });
        try {
            AiServiceClient client = new AiServiceClient(RestClient.builder(),
                "http://127.0.0.1:" + server.getAddress().getPort(), Duration.ofMinutes(1), Duration.ofMillis(300));

            assertThat(client.coach("hola", null)).isEmpty();
            assertThat(client.coach("otra vez", null)).isEmpty();
            assertThat(coachCalls.get()).as("coach is paused after its timeout").isEqualTo(1);

            assertThat(client.analyze(frame())).isPresent();
            assertThat(analyzeCalls.get()).isEqualTo(1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void coachServerErrorDoesNotPauseAnalyzeAndAnalyzeErrorDoesNotPauseCoach() throws IOException {
        AtomicInteger coachCalls = new AtomicInteger();
        AtomicInteger analyzeCalls = new AtomicInteger();
        HttpServer server = server("/coach", exchange -> {
            coachCalls.incrementAndGet();
            respond(exchange, 503, "{}");
        });
        server.createContext("/analyze", exchange -> {
            analyzeCalls.incrementAndGet();
            respond(exchange, 503, "{}");
        });
        try {
            AiServiceClient client = new AiServiceClient(RestClient.builder(),
                "http://127.0.0.1:" + server.getAddress().getPort(), Duration.ofMinutes(1), AiServiceClient.COACH_TIMEOUT);

            assertThat(client.coach("hola", null)).isEmpty();
            assertThat(client.analyze(frame())).isEmpty();
            // Each breaker tripped on its own failure: no further calls to either operation.
            assertThat(client.coach("hola", null)).isEmpty();
            assertThat(client.analyze(frame())).isEmpty();
            assertThat(coachCalls.get()).isEqualTo(1);
            assertThat(analyzeCalls.get()).isEqualTo(1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void analyzeFailureDoesNotPauseCoach() throws IOException {
        AtomicInteger coachCalls = new AtomicInteger();
        HttpServer server = server("/analyze", exchange -> respond(exchange, 503, "{}"));
        server.createContext("/coach", exchange -> {
            coachCalls.incrementAndGet();
            respond(exchange, "{\"answer\":\"Descansa.\",\"source\":\"rules\"}");
        });
        try {
            AiServiceClient client = new AiServiceClient(RestClient.builder(),
                "http://127.0.0.1:" + server.getAddress().getPort(), Duration.ofMinutes(1), AiServiceClient.COACH_TIMEOUT);

            assertThat(client.analyze(frame())).isEmpty();
            assertThat(client.coach("hola", null)).isPresent();
            assertThat(coachCalls.get()).isEqualTo(1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void sharedSecretIsSentAsInternalTokenOnAnalyzeAndCoach() throws IOException {
        AtomicReference<String> analyzeToken = new AtomicReference<>();
        AtomicReference<String> coachToken = new AtomicReference<>();
        HttpServer server = server("/analyze", exchange -> {
            analyzeToken.set(exchange.getRequestHeaders().getFirst("X-Internal-Token"));
            respond(exchange, "{\"score\":80,\"feedback\":[],\"analyzed\":true,\"phase\":\"down\",\"reps\":1,\"message\":\"ok\"}");
        });
        server.createContext("/coach", exchange -> {
            coachToken.set(exchange.getRequestHeaders().getFirst("X-Internal-Token"));
            respond(exchange, "{\"answer\":\"Descansa.\",\"source\":\"rules\"}");
        });
        try {
            AiServiceClient client = clientWithSecret(server.getAddress().getPort(), "s3cret-value");

            assertThat(client.analyze(frame())).isPresent();
            assertThat(client.coach("hola", null)).isPresent();
            assertThat(analyzeToken.get()).isEqualTo("s3cret-value");
            assertThat(coachToken.get()).isEqualTo("s3cret-value");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void noInternalTokenIsSentWhenSharedSecretIsMissingOrBlank() throws IOException {
        AtomicReference<String> token = new AtomicReference<>();
        HttpServer server = server("/analyze", exchange -> {
            token.set(exchange.getRequestHeaders().getFirst("X-Internal-Token"));
            respond(exchange, "{\"score\":80,\"feedback\":[],\"analyzed\":true,\"phase\":\"down\",\"reps\":1,\"message\":\"ok\"}");
        });
        try {
            int port = server.getAddress().getPort();

            assertThat(client(port).analyze(frame())).isPresent();
            assertThat(token.get()).isNull();

            assertThat(clientWithSecret(port, "   ").analyze(frame())).isPresent();
            assertThat(token.get()).isNull();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void unauthorizedFromAiFallsBackAndPausesLikeAnyFailure() throws IOException {
        AtomicInteger analyzeCalls = new AtomicInteger();
        HttpServer server = server("/analyze", exchange -> {
            analyzeCalls.incrementAndGet();
            respond(exchange, 401, "{\"detail\":\"unauthorized\"}");
        });
        server.createContext("/coach", exchange -> respond(exchange, 401, "{\"detail\":\"unauthorized\"}"));
        try {
            AiServiceClient client = clientWithSecret(server.getAddress().getPort(), "wrong-secret");
            AiServicePoseStrategy strategy = new AiServicePoseStrategy(client, new MediaPipePoseStrategy());

            assertThat(strategy.analyze(frame()).engine()).isEqualTo("local");
            assertThat(strategy.analyze(frame()).engine()).isEqualTo("local");
            assertThat(client.coach("hola", null)).isEmpty();
            assertThat(analyzeCalls.get()).as("a 401 pauses the operation during the cool-down").isEqualTo(1);
        } finally {
            server.stop(0);
        }
    }

    private static AiServiceClient clientWithSecret(int port, String sharedSecret) {
        return new AiServiceClient(RestClient.builder(), "http://127.0.0.1:" + port, Duration.ofMinutes(1),
            AiServiceClient.COACH_TIMEOUT, sharedSecret);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static AiServicePoseStrategy strategy(int port) {
        return new AiServicePoseStrategy(client(port), new MediaPipePoseStrategy());
    }

    private static AiServiceClient client(int port) {
        return new AiServiceClient(RestClient.builder(), "http://127.0.0.1:" + port, Duration.ZERO, AiServiceClient.COACH_TIMEOUT);
    }

    private static PoseFrame frame() {
        return new PoseFrame(List.of(new PosePoint("left_hip", 0.5, 0.4, 0.92)), "squat", 1280, 720, "session-7");
    }

    private static HttpServer server(String path, com.sun.net.httpserver.HttpHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        // One thread per request: a slow handler must not block the other endpoint in the same test.
        server.setExecutor(Executors.newCachedThreadPool(task -> {
            Thread thread = new Thread(task);
            thread.setDaemon(true);
            return thread;
        }));
        server.createContext(path, handler);
        server.start();
        return server;
    }

    private static String read(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, String json) throws IOException {
        respond(exchange, 200, json);
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String json) throws IOException {
        read(exchange);
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
