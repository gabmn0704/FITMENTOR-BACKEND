package com.fitmentor.api;

import java.util.List;

public record DashboardResponse(String greeting, int weeklySessions, double averageAccuracy, int streak, List<String> nextRoutine) {}
