package com.fitmentor.domain.exercise;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Catalog of exercises and their progressions. Products come from {@link ExerciseFactory}.
 */
@Service
public class ExerciseCatalog {

    private final List<Exercise> exercises = List.copyOf(ExerciseFactory.createAll());

    public List<Exercise> all() {
        return exercises;
    }

    public Exercise require(String id) {
        return find(id).orElseThrow(() -> new NoSuchElementException("Unknown exercise: " + id));
    }

    public Optional<Exercise> find(String id) {
        return exercises.stream().filter(exercise -> exercise.id().equals(id)).findFirst();
    }

    /**
     * Case- and accent-insensitive text search over id, label and muscle.
     * A blank query returns the whole catalog.
     */
    public List<Exercise> search(String query) {
        String needle = normalize(query);
        if (needle.isEmpty()) {
            return exercises;
        }
        return exercises.stream()
            .filter(exercise -> normalize(exercise.id()).contains(needle)
                || normalize(exercise.label()).contains(needle)
                || normalize(exercise.muscle()).contains(needle))
            .toList();
    }

    /**
     * Progressions from the given exercise: harder exercises only, ordered by distance
     * (difference of difficulty ranks) and then by id.
     */
    public List<Progression> next(String fromId) {
        Exercise from = require(fromId);
        return exercises.stream()
            .filter(candidate -> candidate.rank() > from.rank())
            .map(candidate -> new Progression(candidate, candidate.rank() - from.rank()))
            .sorted(Comparator.comparingInt(Progression::distance).thenComparing(p -> p.exercise().id()))
            .toList();
    }

    /** A harder exercise reachable from another one, with its distance in difficulty ranks. */
    public record Progression(Exercise exercise, int distance) {
    }

    /** Lowercases and removes diacritics so that "flexion" matches "Flexión". */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(text.trim(), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
