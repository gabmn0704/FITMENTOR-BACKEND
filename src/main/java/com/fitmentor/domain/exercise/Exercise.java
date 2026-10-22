package com.fitmentor.domain.exercise;

import java.util.List;

/**
 * Product created by {@link ExerciseFactory}: an exercise of the catalog.
 *
 * <p>Each concrete subclass (Squat, PushUp, Plank, Deadlift, Lunge) defines its own
 * metabolic equivalent (MET) and its own posture rules. The local pose analyzer and the
 * calorie calculation work on this abstraction, so they never branch on the exercise id.
 */
public abstract class Exercise {

    /** Stable identifier used by the API and the frontend, e.g. {@code push_up}. */
    public abstract String id();

    public abstract String label();

    public abstract String muscle();

    public abstract Difficulty difficulty();

    /** Metabolic equivalent of the task, used for calorie estimation. */
    public abstract double met();

    /** Posture rules applied by the local (fallback) analyzer. */
    public abstract List<AngleRule> postureRules();

    /** Rank used for progressions: a harder exercise has a higher rank. */
    public final int rank() {
        return difficulty().ordinal();
    }
}
