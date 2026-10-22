package com.fitmentor.domain.routine;

import com.fitmentor.domain.user.FitnessLevel;

import java.util.List;
import java.util.Objects;

/**
 * Template Method pattern: {@link #build(RoutineContext)} is the fixed algorithm
 * (validate, select exercises, personalize items, choose rest, explain). Subclasses
 * provide the varying steps, so a new routine policy only overrides those hooks.
 */
public abstract class RoutineTemplate {

    /** Final template method: the skeleton of the algorithm, not meant to be overridden. */
    public final RoutinePlan build(RoutineContext context) {
        validate(context);
        List<String> exercises = selectExercises(context.level());
        List<RoutinePlan.RoutineItem> items = personalize(exercises, context);
        int rest = restSeconds(context);
        return new RoutinePlan(context.level().name(), rest, items, explain(context));
    }

    protected void validate(RoutineContext context) {
        Objects.requireNonNull(context, "context is required");
        Objects.requireNonNull(context.level(), "level is required");
    }

    /** Exercise ids for the level, from the easiest to the hardest. */
    protected abstract List<String> selectExercises(FitnessLevel level);

    /** Sets, reps and note for each selected exercise. */
    protected abstract List<RoutinePlan.RoutineItem> personalize(List<String> exercises, RoutineContext context);

    /** Rest between sets in seconds. */
    protected abstract int restSeconds(RoutineContext context);

    /** Human-readable reason of the adjustment, shown to the user. */
    protected abstract String explain(RoutineContext context);
}
