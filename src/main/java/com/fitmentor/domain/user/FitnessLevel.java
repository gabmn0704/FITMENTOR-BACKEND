package com.fitmentor.domain.user;

import java.util.Locale;

/** Self-declared training level of a user. Drives the base routine. */
public enum FitnessLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED;

    /** Parses a level name case-insensitively; returns null for null or blank input. */
    public static FitnessLevel parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
