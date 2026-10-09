package com.fitmentor.domain.pose;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class MediaPipePoseStrategy implements PoseAnalysisStrategy {
    private final RestClient aiClient;

    public MediaPipePoseStrategy(@Value("${ai.service-url:http://localhost:8000}") String serviceUrl) {
        String baseUrl = serviceUrl.contains("://") ? serviceUrl : "http://" + serviceUrl;
        this.aiClient = RestClient.builder()
            .baseUrl(baseUrl)
            .build();
    }

    @Override
    public PoseAnalysis analyze(PoseFrame frame) {
        try {
            PoseServiceResult result = aiClient.post()
                .uri("/analyze")
                .body(frame)
                .retrieve()
                .body(PoseServiceResult.class);
            if (result == null) {
                return unavailable(frame);
            }
            String feedback = result.feedback() == null || result.feedback().isEmpty()
                ? "Keep your movement controlled and try again."
                : String.join(" ", result.feedback());
            return new PoseAnalysis(result.score(), feedback, frame.points());
        } catch (RestClientException exception) {
            return unavailable(frame);
        }
    }

    @Override
    public String engine() {
        return "mediapipe";
    }

    private PoseAnalysis unavailable(PoseFrame frame) {
        return new PoseAnalysis(0, "Pose analysis is temporarily unavailable. Please try again.", frame.points());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PoseServiceResult(double score, List<String> feedback) {}
}
