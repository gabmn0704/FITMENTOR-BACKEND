package com.fitmentor.domain.pose;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

@Component
public class AiServicePoseStrategy implements PoseAnalysisStrategy {
    private static final Logger logger = LoggerFactory.getLogger(AiServicePoseStrategy.class);

    private final RestClient aiClient;
    private final MediaPipePoseStrategy fallback;

    public AiServicePoseStrategy(
        RestClient.Builder clientBuilder,
        @Value("${ai.service-url}") String aiServiceUrl,
        MediaPipePoseStrategy fallback
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(4));
        this.aiClient = clientBuilder
            .baseUrl(aiServiceUrl)
            .requestFactory(requestFactory)
            .build();
        this.fallback = fallback;
    }

    @Override
    public PoseAnalysis analyze(PoseFrame frame) {
        try {
            AiPoseResponse response = aiClient.post()
                .uri("/analyze")
                .body(frame)
                .retrieve()
                .body(AiPoseResponse.class);
            if (response == null || response.feedback() == null || response.feedback().isEmpty()) {
                return fallback.analyze(frame);
            }
            return new PoseAnalysis(response.score(), String.join(" ", response.feedback()), frame.points());
        } catch (RestClientException exception) {
            logger.warn("AI pose service unavailable; using local analysis: {}", exception.getMessage());
            return fallback.analyze(frame);
        }
    }

    @Override
    public String engine() {
        return "ai-service";
    }
}