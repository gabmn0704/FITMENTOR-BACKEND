package com.fitmentor.domain.routine;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AdaptiveRoutine extends RoutineTemplate {
    @Override
    protected List<String> selectExercises(String level) {
        return switch (level.toUpperCase()) {
            case "ADVANCED" -> List.of("Deadlift", "Push-up", "Plank", "Squat");
            case "INTERMEDIATE" -> List.of("Squat", "Push-up", "Plank");
            default -> List.of("Squat", "Plank");
        };
    }

    @Override
    protected List<String> personalize(List<String> exercises, String level) {
        return exercises.stream().map(exercise -> exercise + ": " + (level.equalsIgnoreCase("ADVANCED") ? 4 : 3) + " sets").toList();
    }
}
