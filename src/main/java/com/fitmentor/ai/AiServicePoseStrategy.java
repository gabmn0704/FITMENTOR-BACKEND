package com.fitmentor.ai;

import com.fitmentor.domain.pose.MediaPipePoseStrategy;
import com.fitmentor.domain.pose.PoseAnalysis;
import com.fitmentor.domain.pose.PoseAnalysisStrategy;
import com.fitmentor.domain.pose.PoseFrame;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Strategy: analysis by the AI service, the source of truth for repetitions and phase.
 * When the service does not answer, it delegates to the local {@link MediaPipePoseStrategy}.
 */
@Component
public class AiServicePoseStrategy implements PoseAnalysisStrategy {

    public static final String ENGINE = "ai-service";

    private final AiServiceClient client;
    private final MediaPipePoseStrategy fallback;

    public AiServicePoseStrategy(AiServiceClient client, MediaPipePoseStrategy fallback) {
        this.client = client;
        this.fallback = fallback;
    }

    @Override
    public PoseAnalysis analyze(PoseFrame frame) {
        return client.analyze(frame)
            .filter(AiPoseResponse::analyzed)
            .map(response -> toAnalysis(response))
            .orElseGet(() -> fallback.analyze(frame));
    }

    @Override
    public String engine() {
        return ENGINE;
    }

    private static PoseAnalysis toAnalysis(AiPoseResponse response) {
        List<String> feedback = response.feedback().isEmpty()
            ? List.of(response.message() == null ? "Sigue así." : response.message())
            : response.feedback();
        return new PoseAnalysis(response.score(), feedback, response.phase(), response.reps(), ENGINE);
    }
}
