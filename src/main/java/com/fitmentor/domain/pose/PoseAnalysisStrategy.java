package com.fitmentor.domain.pose;

public interface PoseAnalysisStrategy {
    PoseAnalysis analyze(PoseFrame frame);
    String engine();
}
