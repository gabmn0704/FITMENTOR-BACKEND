package com.fitmentor.ai;

import com.fitmentor.domain.pose.PoseFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Optional;

/**
 * Adapter: exposes the AI service HTTP API (RestClient, 1500 ms timeout) as plain Java
 * methods. Callers never see HTTP, URLs or JSON; failures become {@link Optional#empty()}
 * so the caller can fall back to local analysis.
 */
@Component
public class AiServiceClient {

    static final Duration TIMEOUT = Duration.ofMillis(1500);

    private static final Logger logger = LoggerFactory.getLogger(AiServiceClient.class);

    private final RestClient http;

    public AiServiceClient(RestClient.Builder builder, @Value("${ai.service-url}") String serviceUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT);
        requestFactory.setReadTimeout(TIMEOUT);
        this.http = builder
            .baseUrl(withScheme(serviceUrl))
            .requestFactory(requestFactory)
            .build();
    }

    /** Asks the AI service to analyze a frame. Empty when the service is unavailable. */
    public Optional<AiPoseResponse> analyze(PoseFrame frame) {
        try {
            return Optional.ofNullable(http.post()
                .uri("/analyze")
                .body(frame)
                .retrieve()
                .body(AiPoseResponse.class));
        } catch (RestClientException exception) {
            logger.warn("AI pose service unavailable: {}", exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    /** Asks the AI coach a question. Empty when the service is unavailable. */
    public Optional<String> coach(String question, String context) {
        try {
            AiCoachResponse response = http.post()
                .uri("/coach")
                .body(new AiCoachRequest(question, context == null ? "" : context))
                .retrieve()
                .body(AiCoachResponse.class);
            return response == null || response.answer() == null || response.answer().isBlank()
                ? Optional.empty()
                : Optional.of(response.answer());
        } catch (RestClientException exception) {
            logger.warn("AI coach service unavailable: {}", exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    static String withScheme(String url) {
        return url.contains("://") ? url : "http://" + url;
    }

    record AiCoachRequest(String question, String context) {
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    record AiCoachResponse(String answer) {
    }
}
