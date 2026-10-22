package com.fitmentor.domain.achievement;

import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Evaluates every {@link AchievementRule} against the user's aggregates. The standard
 * set is built with the rules below; each rule is a small strategy that reports progress.
 */
public final class AchievementEvaluator {

    static final int COMPLETE = 100;

    private final List<AchievementRule> rules;

    public AchievementEvaluator(List<AchievementRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public static AchievementEvaluator standard() {
        return new AchievementEvaluator(List.of(
            countRule("first_session", "Primera sesión", "Completa tu primera sesión.", 1,
                AchievementInput::totalSessions),
            countRule("reps_100", "100 repeticiones", "Acumula 100 repeticiones.", 100,
                AchievementInput::totalReps),
            countRule("streak_3", "Racha de 3 días", "Entrena 3 días seguidos.", 3,
                AchievementInput::streakDays),
            new AccuracyRule(),
            countRule("five_exercises", "5 ejercicios distintos", "Practica 5 ejercicios diferentes.", 5,
                AchievementInput::distinctExercises)
        ));
    }

    public List<AchievementStatus> evaluate(AchievementInput input) {
        return rules.stream()
            .map(rule -> {
                int progress = clamp(rule.progress(input));
                return new AchievementStatus(rule.id(), rule.title(), rule.description(), progress == COMPLETE, progress);
            })
            .toList();
    }

    private static AchievementRule countRule(String id, String title, String description, int target,
                                             ToIntFunction<AchievementInput> counter) {
        return new AchievementRule() {
            @Override public String id() { return id; }
            @Override public String title() { return title; }
            @Override public String description() { return description; }
            @Override public int progress(AchievementInput input) {
                return (int) Math.min(COMPLETE, (long) counter.applyAsInt(input) * COMPLETE / target);
            }
        };
    }

    static int clamp(int value) {
        return Math.max(0, Math.min(COMPLETE, value));
    }

    /** Precision goal: best single-session accuracy compared with 90%. */
    private static final class AccuracyRule implements AchievementRule {
        @Override public String id() { return "accuracy_90"; }
        @Override public String title() { return "Precisión 90%+"; }
        @Override public String description() { return "Consigue una sesión con 90% de precisión o más."; }
        @Override public int progress(AchievementInput input) {
            return (int) Math.round(Math.min(input.bestAccuracy(), 90.0) / 90.0 * COMPLETE);
        }
    }
}
