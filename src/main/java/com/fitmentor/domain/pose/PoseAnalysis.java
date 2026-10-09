package com.fitmentor.domain.pose;

import java.util.List;

public record PoseAnalysis(double score, String feedback, List<PosePoint> points) {}
