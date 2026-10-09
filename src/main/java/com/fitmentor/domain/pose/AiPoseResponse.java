package com.fitmentor.domain.pose;

import java.util.List;

public record AiPoseResponse(double score, List<String> feedback, boolean analyzed) {}