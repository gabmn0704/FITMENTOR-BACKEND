package com.fitmentor.domain.pose;

import java.util.List;

public record PoseFrame(List<PosePoint> points, String exercise) {}
