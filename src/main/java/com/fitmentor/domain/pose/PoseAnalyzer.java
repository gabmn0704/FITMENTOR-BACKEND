package com.fitmentor.domain.pose;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PoseAnalyzer {
    private final Map<String, PoseAnalysisStrategy> strategies;

    public PoseAnalyzer(java.util.List<PoseAnalysisStrategy> strategies) {
        this.strategies = strategies.stream().collect(Collectors.toMap(strategy -> strategy.engine(), strategy -> strategy));
    }

    public PoseAnalysis analyze(PoseFrame frame, String engine) {
        PoseAnalysisStrategy strategy = strategies.getOrDefault(engine, strategies.get("mediapipe"));
        return strategy.analyze(frame);
    }
}
