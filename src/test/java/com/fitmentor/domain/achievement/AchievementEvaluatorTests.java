package com.fitmentor.domain.achievement;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AchievementEvaluatorTests {

    private final AchievementEvaluator evaluator = AchievementEvaluator.standard();

    @Test
    void noActivityUnlocksNothing() {
        List<AchievementStatus> statuses = evaluator.evaluate(new AchievementInput(0, 0, 0, 0.0, 0));

        assertThat(statuses).hasSize(5);
        assertThat(statuses).allSatisfy(status -> {
            assertThat(status.unlocked()).isFalse();
            assertThat(status.progress()).isZero();
        });
    }

    @Test
    void progressIsPartialUntilTheTargetIsReached() {
        List<AchievementStatus> statuses = evaluator.evaluate(new AchievementInput(0, 50, 1, 45.0, 2));

        assertThat(byId(statuses, "reps_100")).isEqualTo(new AchievementStatus("reps_100", "100 repeticiones", "Acumula 100 repeticiones.", false, 50));
        assertThat(byId(statuses, "streak_3").progress()).isEqualTo(33);
        assertThat(byId(statuses, "accuracy_90").progress()).isEqualTo(50);
        assertThat(byId(statuses, "five_exercises").progress()).isEqualTo(40);
    }

    @Test
    void unlocksWhenProgressReachesOneHundred() {
        List<AchievementStatus> statuses = evaluator.evaluate(new AchievementInput(1, 150, 3, 95.0, 5));

        assertThat(statuses).allSatisfy(status -> {
            assertThat(status.unlocked()).isTrue();
            assertThat(status.progress()).isEqualTo(100);
        });
    }

    @Test
    void progressNeverExceedsOneHundred() {
        AchievementRule overflow = new AchievementRule() {
            @Override public String id() { return "overflow"; }
            @Override public String title() { return "Overflow"; }
            @Override public String description() { return "Always too high."; }
            @Override public int progress(AchievementInput input) { return 250; }
        };

        AchievementStatus status = new AchievementEvaluator(List.of(overflow))
            .evaluate(new AchievementInput(0, 0, 0, 0, 0)).get(0);

        assertThat(status.progress()).isEqualTo(100);
        assertThat(status.unlocked()).isTrue();
    }

    private static AchievementStatus byId(List<AchievementStatus> statuses, String id) {
        return statuses.stream().filter(status -> status.id().equals(id)).findFirst().orElseThrow();
    }
}
