package com.fitmentor.domain.routine;

import java.util.List;

public abstract class RoutineTemplate {
    public final List<String> build(String level) {
        validate(level);
        List<String> exercises = selectExercises(level);
        return personalize(exercises, level);
    }

    protected void validate(String level) {
        if (level == null || level.isBlank()) throw new IllegalArgumentException("Level is required");
    }

    protected abstract List<String> selectExercises(String level);
    protected abstract List<String> personalize(List<String> exercises, String level);
}
