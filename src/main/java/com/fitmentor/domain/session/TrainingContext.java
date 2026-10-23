package com.fitmentor.domain.session;

import java.util.Objects;

/**
 * Mutable context of one live session: current state and repetition count.
 * The Memento is produced by {@link #save()} and consumed by {@link #restore(Memento)},
 * which is how the command invoker implements undo without exposing internals.
 */
public class TrainingContext {

    private TrainingState state;
    private int repetitions;

    public TrainingContext(TrainingState initialState) {
        this.state = Objects.requireNonNull(initialState);
    }

    public void transitionTo(TrainingState nextState) {
        state = Objects.requireNonNull(nextState);
    }

    public void countRepetition() {
        repetitions++;
    }

    /** Repetitions reported by the analyzer (the AI service is the source of truth). */
    public void reportRepetitions(int reps) {
        if (reps < 0) {
            throw new IllegalArgumentException("Repetitions must not be negative");
        }
        repetitions = reps;
    }

    public TrainingState state() {
        return state;
    }

    public int repetitions() {
        return repetitions;
    }

    public Memento save() {
        return new Memento(state, repetitions);
    }

    public void restore(Memento memento) {
        state = memento.state();
        repetitions = memento.repetitions();
    }

    /** Immutable snapshot of a context. */
    public record Memento(TrainingState state, int repetitions) {
    }
}
