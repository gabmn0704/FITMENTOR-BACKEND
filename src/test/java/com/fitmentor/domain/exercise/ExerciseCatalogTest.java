package com.fitmentor.domain.exercise;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExerciseCatalogTest {

    private final ExerciseCatalog catalog = new ExerciseCatalog();

    @Test
    void searchIgnoresCaseAndAccents() {
        assertThat(ids(catalog.search("PUSH"))).containsExactly("push_up");
        assertThat(ids(catalog.search("flexion"))).isEmpty();
        assertThat(ExerciseCatalog.normalize("Flexión")).isEqualTo("flexion");
    }

    @Test
    void searchMatchesMuscleAndReturnsAllForBlankQuery() {
        assertThat(ids(catalog.search("legs"))).containsExactly("squat", "lunge");
        assertThat(catalog.search("   ")).hasSize(5);
        assertThat(catalog.search(null)).hasSize(5);
    }

    @Test
    void progressionsAreHarderExercisesOrderedByDistance() {
        List<ExerciseCatalog.Progression> steps = catalog.next("plank");

        assertThat(steps).extracting(step -> step.exercise().id())
            .containsExactly("lunge", "squat", "deadlift");
        assertThat(steps).extracting(ExerciseCatalog.Progression::distance).containsExactly(1, 1, 2);
    }

    @Test
    void hardestExerciseHasNoProgression() {
        assertThat(catalog.next("deadlift")).isEmpty();
    }

    @Test
    void unknownStartingExerciseIsRejected() {
        assertThatThrownBy(() -> catalog.next("burpee")).isInstanceOf(NoSuchElementException.class);
    }

    private static List<String> ids(List<Exercise> exercises) {
        return exercises.stream().map(Exercise::id).toList();
    }
}
