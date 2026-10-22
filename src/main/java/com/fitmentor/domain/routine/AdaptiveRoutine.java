package com.fitmentor.domain.routine;

import com.fitmentor.domain.user.FitnessLevel;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Concrete step of the Template Method: sets and rest rise when accuracy is high and the
 * user trains consistently, and fall when accuracy is low.
 */
@Component
public class AdaptiveRoutine extends RoutineTemplate {

    static final int MIN_SETS = 1;
    static final int MAX_SETS = 5;
    static final int MIN_REST = 45;
    static final int MAX_REST = 150;
    static final double HIGH_ACCURACY = 90.0;
    static final double LOW_ACCURACY = 70.0;
    static final int CONSISTENT_DAYS = 3;

    @Override
    protected List<String> selectExercises(FitnessLevel level) {
        return switch (level) {
            case BEGINNER -> List.of("plank", "push_up", "lunge");
            case INTERMEDIATE -> List.of("squat", "push_up", "plank", "lunge");
            case ADVANCED -> List.of("deadlift", "squat", "push_up", "plank");
        };
    }

    @Override
    protected List<RoutinePlan.RoutineItem> personalize(List<String> exercises, RoutineContext context) {
        int sets = clamp(baseSets(context.level()) + setDelta(context), MIN_SETS, MAX_SETS);
        int reps = baseReps(context.level());
        String note = noteFor(context);
        return exercises.stream()
            .map(exercise -> new RoutinePlan.RoutineItem(exercise, sets, reps, note))
            .toList();
    }

    @Override
    protected int restSeconds(RoutineContext context) {
        int rest = baseRest(context.level());
        if (isHighAccuracyConsistent(context)) {
            rest -= 15;
        } else if (isLowAccuracy(context)) {
            rest += 15;
        }
        return clamp(rest, MIN_REST, MAX_REST);
    }

    @Override
    protected String explain(RoutineContext context) {
        if (!context.hasHistory()) {
            return "Sin historial todavía: rutina base para tu nivel.";
        }
        if (isHighAccuracyConsistent(context)) {
            return "Precisión alta y entrenas con constancia: subimos series y bajamos el descanso.";
        }
        if (isLowAccuracy(context)) {
            return "Precisión baja: bajamos series y aumentamos el descanso para cuidar la técnica.";
        }
        return "Rendimiento estable: mantenemos la rutina.";
    }

    private int setDelta(RoutineContext context) {
        if (isHighAccuracyConsistent(context)) {
            return 1;
        }
        return isLowAccuracy(context) ? -1 : 0;
    }

    private String noteFor(RoutineContext context) {
        if (!context.hasHistory()) {
            return "Mantén una técnica controlada.";
        }
        if (isLowAccuracy(context)) {
            return "Prioriza la forma sobre la velocidad.";
        }
        return isHighAccuracyConsistent(context) ? "Cierra cada repetición con control." : "Busca precisión constante.";
    }

    private static boolean isHighAccuracyConsistent(RoutineContext context) {
        return context.hasHistory()
            && context.avgAccuracy() >= HIGH_ACCURACY
            && context.activeDaysInWeek() >= CONSISTENT_DAYS;
    }

    private static boolean isLowAccuracy(RoutineContext context) {
        return context.hasHistory() && context.avgAccuracy() < LOW_ACCURACY;
    }

    private static int baseSets(FitnessLevel level) {
        return switch (level) {
            case BEGINNER -> 2;
            case INTERMEDIATE -> 3;
            case ADVANCED -> 4;
        };
    }

    private static int baseReps(FitnessLevel level) {
        return switch (level) {
            case BEGINNER -> 8;
            case INTERMEDIATE, ADVANCED -> 10;
        };
    }

    private static int baseRest(FitnessLevel level) {
        return switch (level) {
            case BEGINNER -> 90;
            case INTERMEDIATE -> 75;
            case ADVANCED -> 60;
        };
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
