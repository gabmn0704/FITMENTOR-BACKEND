package com.fitmentor.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fitmentor.domain.pose.PoseFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Optional;

/**
 * Adapter: exposes the AI service HTTP API (RestClient, 1500 ms timeout) as plain Java
 * methods. Callers never see HTTP, URLs or JSON; failures become {@link Optional#empty()}
 * so the caller can fall back to local analysis or rule-based answers.
 *
 * <p>A 4xx answer (the service returns 422 when a frame or question fails its validation)
 * only drops that request. Connection errors, timeouts and 5xx answers also pause calls for
 * a short cool-down, so a dead service does not add a timeout to every frame of a session.
 */
@Component
public class AiServiceClient {

    static final Duration TIMEOUT = Duration.ofMillis(1500);
    static final Duration COOL_DOWN = Duration.ofSeconds(5);

    private static final Logger logger = LoggerFactory.getLogger(AiServiceClient.class);

    private final RestClient http;
    private final long coolDownNanos;
    private volatile long pausedUntilNanos = Long.MIN_VALUE;
    private volatile boolean paused;

    @Autowired
    public AiServiceClient(RestClient.Builder builder, @Value("${ai.service-url}") String serviceUrl) {
        this(builder, serviceUrl, COOL_DOWN);
    }

    AiServiceClient(RestClient.Builder builder, String serviceUrl, Duration coolDown) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT);
        requestFactory.setReadTimeout(TIMEOUT);
        this.http = builder
            .baseUrl(withScheme(serviceUrl))
            .requestFactory(requestFactory)
            .build();
        this.coolDownNanos = coolDown.toNanos();
    }

    /** Asks the AI service to analyze a frame. Empty when the service is unavailable or rejects it. */
    public Optional<AiPoseResponse> analyze(PoseFrame frame) {
        if (isPaused()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(http.post()
                .uri("/analyze")
                .body(frame)
                .retrieve()
                .body(AiPoseResponse.class));
        } catch (RestClientException exception) {
            failed("pose", exception);
            return Optional.empty();
        }
    }

    /** Asks the AI coach a question. Empty when the service is unavailable or rejects it. */
    public Optional<CoachReply> coach(String question, CoachContext context) {
        if (isPaused()) {
            return Optional.empty();
        }
        try {
            CoachReply reply = http.post()
                .uri("/coach")
                .body(new CoachRequest(question, context))
                .retrieve()
                .body(CoachReply.class);
            return reply == null || reply.answer() == null || reply.answer().isBlank()
                ? Optional.empty()
                : Optional.of(reply);
        } catch (RestClientException exception) {
            failed("coach", exception);
            return Optional.empty();
        }
    }

    private boolean isPaused() {
        if (!paused) {
            return false;
        }
        if (System.nanoTime() - pausedUntilNanos >= 0) {
            paused = false;
            return false;
        }
        return true;
    }

    private void failed(String operation, RestClientException exception) {
        if (exception instanceof HttpClientErrorException clientError) {
            // Validation rejections (422) and other 4xx: the service is up, only this request is dropped.
            logger.debug("AI {} request rejected with status {}", operation, clientError.getStatusCode().value());
            return;
        }
        pausedUntilNanos = System.nanoTime() + coolDownNanos;
        paused = true;
        logger.warn("AI {} service unavailable ({}); using the local fallback for {} s",
            operation, exception.getClass().getSimpleName(), Duration.ofNanos(coolDownNanos).toSeconds());
    }

    static String withScheme(String url) {
        return url.contains("://") ? url : "http://" + url;
    }

    /** Structured context accepted by the AI {@code /coach} endpoint. Null fields are omitted. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CoachContext(String exercise, Double score, Integer reps, String level) {
    }

    /** Answer of the AI {@code /coach} endpoint; {@code source} is "claude" or "rules". */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CoachReply(String answer, String source) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record CoachRequest(String question, CoachContext context) {
    }
}
