package com.fitmentor.domain.feedback;

public record FeedbackEvent(String type, String message, double score, int repetitions) {}
