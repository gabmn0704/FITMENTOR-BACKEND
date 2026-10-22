package com.fitmentor.domain.exercise;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Factory with a registry of creators (a parameterized factory, close to Factory Method).
 *
 * <p>Callers ask for an exercise by id and never call {@code new Squat()} themselves.
 * Each id is bound to a creator ({@code Supplier}) that instantiates the concrete
 * product. Adding an exercise means adding one class and one registry entry, without
 * touching the catalog, the analyzer or the calorie code.
 *
 * <p>Honest note: the creators are suppliers, not subclasses overriding a creator method, so
 * this is the "parameterized factory" variant rather than the class-based GoF Factory Method.
 */
public final class ExerciseFactory {

    private static final Map<String, Supplier<Exercise>> CREATORS = new LinkedHashMap<>();

    static {
        register(Exercises.Squat::new);
        register(Exercises.PushUp::new);
        register(Exercises.Plank::new);
        register(Exercises.Deadlift::new);
        register(Exercises.Lunge::new);
    }

    private ExerciseFactory() {
    }

    private static void register(Supplier<Exercise> creator) {
        CREATORS.put(creator.get().id(), creator);
    }

    /** Creates the exercise with the given id, or empty when the id is unknown. */
    public static Optional<Exercise> create(String id) {
        Supplier<Exercise> creator = id == null ? null : CREATORS.get(id);
        return creator == null ? Optional.empty() : Optional.of(creator.get());
    }

    /** Creates one instance of every registered exercise, in registration order. */
    public static Collection<Exercise> createAll() {
        return CREATORS.values().stream().map(Supplier::get).toList();
    }
}
