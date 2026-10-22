package com.fitmentor.domain.routine;

import com.fitmentor.domain.user.FitnessLevel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdaptiveRoutineTests {

    private final RoutineTemplate routine = new AdaptiveRoutine();

    @Test
    void newUserGetsBaseRoutineWithoutHistory() {
        RoutinePlan plan = routine.build(new RoutineContext(FitnessLevel.BEGINNER, null, 0));

        assertThat(plan.level()).isEqualTo("BEGINNER");
        assertThat(plan.restSeconds()).isEqualTo(90);
        assertThat(plan.items()).extracting(RoutinePlan.RoutineItem::exercise).containsExactly("plank", "push_up", "lunge");
        assertThat(plan.items()).allSatisfy(item -> {
            assertThat(item.sets()).isEqualTo(2);
            assertThat(item.reps()).isEqualTo(8);
        });
        assertThat(plan.reason()).contains("Sin historial");
    }

    @Test
    void highAccuracyWithConsistencyRaisesSetsAndShortensRest() {
        RoutinePlan plan = routine.build(new RoutineContext(FitnessLevel.INTERMEDIATE, 93.0, 4));

        assertThat(plan.items()).allSatisfy(item -> assertThat(item.sets()).isEqualTo(4));
        assertThat(plan.restSeconds()).isEqualTo(60);
        assertThat(plan.reason()).contains("subimos series");
    }

    @Test
    void highAccuracyWithoutConsistencyDoesNotAdjust() {
        RoutinePlan plan = routine.build(new RoutineContext(FitnessLevel.INTERMEDIATE, 95.0, 2));

        assertThat(plan.items()).allSatisfy(item -> assertThat(item.sets()).isEqualTo(3));
        assertThat(plan.restSeconds()).isEqualTo(75);
    }

    @Test
    void lowAccuracyLowersSetsAndLengthensRest() {
        RoutinePlan plan = routine.build(new RoutineContext(FitnessLevel.ADVANCED, 60.0, 5));

        assertThat(plan.items()).allSatisfy(item -> assertThat(item.sets()).isEqualTo(3));
        assertThat(plan.restSeconds()).isEqualTo(75);
        assertThat(plan.reason()).contains("bajamos series");
    }

    @Test
    void setsAndRestStayWithinLimits() {
        RoutinePlan advanced = routine.build(new RoutineContext(FitnessLevel.ADVANCED, 99.0, 7));
        RoutinePlan beginner = routine.build(new RoutineContext(FitnessLevel.BEGINNER, 10.0, 1));

        assertThat(advanced.restSeconds()).isGreaterThanOrEqualTo(AdaptiveRoutine.MIN_REST);
        assertThat(beginner.items()).allSatisfy(item -> assertThat(item.sets()).isBetween(AdaptiveRoutine.MIN_SETS, AdaptiveRoutine.MAX_SETS));
        assertThat(beginner.restSeconds()).isLessThanOrEqualTo(AdaptiveRoutine.MAX_REST);
    }

    @Test
    void rejectsMissingLevel() {
        assertThatThrownBy(() -> routine.build(new RoutineContext(null, null, 0)))
            .isInstanceOf(NullPointerException.class);
    }
}
