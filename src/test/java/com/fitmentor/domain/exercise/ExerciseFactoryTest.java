package com.fitmentor.domain.exercise;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExerciseFactoryTest {

    @Test
    void createsConcreteProductForKnownId() {
        Exercise pushUp = ExerciseFactory.create("push_up").orElseThrow();

        assertThat(pushUp).isInstanceOf(Exercises.PushUp.class);
        assertThat(pushUp.label()).isEqualTo("Push-up");
        assertThat(pushUp.met()).isEqualTo(8.0);
    }

    @Test
    void returnsEmptyForUnknownOrNullId() {
        assertThat(ExerciseFactory.create("burpee")).isEmpty();
        assertThat(ExerciseFactory.create(null)).isEmpty();
    }

    @Test
    void createsEveryRegisteredExerciseWithPostureRules() {
        List<String> ids = ExerciseFactory.createAll().stream().map(Exercise::id).toList();

        assertThat(ids).containsExactly("squat", "push_up", "plank", "deadlift", "lunge");
        assertThat(ExerciseFactory.createAll()).allSatisfy(exercise -> assertThat(exercise.postureRules()).isNotEmpty());
    }

    @Test
    void eachCallReturnsNewInstance() {
        assertThat(ExerciseFactory.create("plank").orElseThrow())
            .isNotSameAs(ExerciseFactory.create("plank").orElseThrow());
    }
}
