package com.fitmentor.domain.pose;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Context of the Strategy pattern: selects the analysis strategy by engine name.
 * Unknown engines fall back to the local analyzer.
 *
 * <p>The live socket always asks for {@code ai-service}; that strategy itself falls back to the
 * local one when the AI service fails, so in practice the strategy changes at run time with the
 * availability of the service, frame by frame.
 */
@Service
public class PoseAnalyzer {

    private final Map<String, PoseAnalysisStrategy> strategies;

    public PoseAnalyzer(java.util.List<PoseAnalysisStrategy> strategies) {
        this.strategies = strategies.stream().collect(Collectors.toMap(PoseAnalysisStrategy::engine, strategy -> strategy));
    }

    public PoseAnalysis analyze(PoseFrame frame, String engine) {
        PoseAnalysisStrategy strategy = strategies.getOrDefault(engine, strategies.get(MediaPipePoseStrategy.ENGINE));
        return strategy.analyze(frame);
    }
}
