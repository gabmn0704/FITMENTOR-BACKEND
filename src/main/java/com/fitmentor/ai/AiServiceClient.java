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
 * Adapter: exposes the AI service HTTP API as plain Java methods. Callers never see HTTP, URLs or
 * JSON; failures become {@link Optional#empty()} so the caller can fall back to local analysis or
 * rule-based answers.
 *
 * <p>Each operation has its own timeout and its own cool-down ("breaker"):
 * <ul>
 *   <li>{@code /analyze} (live pose, every frame): 1.5 s. A dead service must not stall the socket.</li>
 *   <li>{@code /coach}: {@code ai.coach-timeout-ms}, 9 s by default. The AI service waits up to 8 s
 *       for Claude, so a shorter timeout would always cut the answer off.</li>
 * </ul>
 * A failure of one operation never pauses the other. A 4xx answer (the service returns 422 when a
 * frame or question fails its validation) only drops that request. A 401 means the shared secret
 * does not match: it counts as a failure (cool-down and a warning, never the secret itself).
 *
 * <p>When {@code ai.shared-secret} is set, every request carries it in the {@code X-Internal-Token}
 * header, which the AI service checks. The value is never logged.
 */
@Component
public class AiServiceClient {

    static final Duration TIMEOUT = Duration.ofMillis(1500);
    static final Duration COACH_TIMEOUT = Duration.ofSeconds(9);
    static final Duration COOL_DOWN = Duration.ofSeconds(5);
    static final String TOKEN_HEADER = "X-Internal-Token";

    private static final Logger logger = LoggerFactory.getLogger(AiServiceClient.class);

    private final RestClient poseHttp;
    private final RestClient coachHttp;
    private final Duration coolDown;
    private final CoolDown poseBreaker;
    private final CoolDown coachBreaker;

    @Autowired
    public AiServiceClient(RestClient.Builder builder,
                           @Value("${ai.service-url}") String serviceUrl,
                           @Value("${ai.coach-timeout-ms:9000}") long coachTimeoutMs,
                           @Value("${ai.shared-secret:}") String sharedSecret) {
        this(builder, serviceUrl, COOL_DOWN,
            coachTimeoutMs > 0 ? Duration.ofMillis(coachTimeoutMs) : COACH_TIMEOUT, sharedSecret);
    }

    AiServiceClient(RestClient.Builder builder, String serviceUrl, Duration coolDown, Duration coachTimeout) {
        this(builder, serviceUrl, coolDown, coachTimeout, "");
    }

    AiServiceClient(RestClient.Builder builder, String serviceUrl, Duration coolDown, Duration coachTimeout,
                    String sharedSecret) {
        String baseUrl = withScheme(serviceUrl);
        String token = sharedSecret == null ? "" : sharedSecret.trim();
        this.poseHttp = client(builder, baseUrl, TIMEOUT, token);
        this.coachHttp = client(builder, baseUrl, coachTimeout, token);
        this.coolDown = coolDown;
        this.poseBreaker = new CoolDown(coolDown);
        this.coachBreaker = new CoolDown(coolDown);
    }

    /** Asks the AI service to analyze a frame. Empty when the service is unavailable or rejects it. */
    public Optional<AiPoseResponse> analyze(PoseFrame frame) {
        if (poseBreaker.isPaused()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(poseHttp.post()
                .uri("/analyze")
                .body(frame)
                .retrieve()
                .body(AiPoseResponse.class));
        } catch (RestClientException exception) {
            failed("pose", poseBreaker, exception);
            return Optional.empty();
        }
    }

    /** Asks the AI coach a question. Empty when the service is unavailable or rejects it. */
    public Optional<CoachReply> coach(String question, CoachContext context) {
        if (coachBreaker.isPaused()) {
            return Optional.empty();
        }
        try {
            CoachReply reply = coachHttp.post()
                .uri("/coach")
                .body(new CoachRequest(question, context))
                .retrieve()
                .body(CoachReply.class);
            return reply == null || reply.answer() == null || reply.answer().isBlank()
                ? Optional.empty()
                : Optional.of(reply);
        } catch (RestClientException exception) {
            failed("coach", coachBreaker, exception);
            return Optional.empty();
        }
    }

    private void failed(String operation, CoolDown breaker, RestClientException exception) {
        if (exception instanceof HttpClientErrorException clientError && clientError.getStatusCode().value() == 401) {
            // Shared secret missing or different on one side: an operator error, treated as a service failure.
            breaker.trip();
            logger.warn("AI {} service rejected the internal token (401); check AI_SHARED_SECRET on both services. "
                + "Using the local fallback for {} s", operation, coolDown.toSeconds());
            return;
        }
        if (exception instanceof HttpClientErrorException clientError) {
            // Validation rejections (422) and other 4xx: the service is up, only this request is dropped.
            logger.debug("AI {} request rejected with status {}", operation, clientError.getStatusCode().value());
            return;
        }
        breaker.trip();
        logger.warn("AI {} service unavailable ({}); using the local fallback for {} s",
            operation, exception.getClass().getSimpleName(), coolDown.toSeconds());
    }

    private static RestClient client(RestClient.Builder builder, String baseUrl, Duration readTimeout, String sharedSecret) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT);
        requestFactory.setReadTimeout(readTimeout);
        RestClient.Builder configured = builder.clone()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory);
        if (!sharedSecret.isEmpty()) {
            configured.defaultHeader(TOKEN_HEADER, sharedSecret);
        }
        return configured.build();
    }

    static String withScheme(String url) {
        return url.contains("://") ? url : "http://" + url;
    }

    /** One cool-down window for one operation. Trips on connection errors, timeouts and 5xx answers. */
    private static final class CoolDown {

        private final long nanos;
        private volatile long pausedUntilNanos = Long.MIN_VALUE;
        private volatile boolean paused;

        CoolDown(Duration duration) {
            this.nanos = duration.toNanos();
        }

        boolean isPaused() {
            if (!paused) {
                return false;
            }
            if (System.nanoTime() - pausedUntilNanos >= 0) {
                paused = false;
                return false;
            }
            return true;
        }

        void trip() {
            pausedUntilNanos = System.nanoTime() + nanos;
            paused = true;
        }
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
