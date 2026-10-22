package com.fitmentor.domain.exercise;

/**
 * Difficulty level of an exercise. The ordinal is used as the rank when computing
 * progressions (distance between two exercises is the difference of ranks).
 */
public enum Difficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED
}
