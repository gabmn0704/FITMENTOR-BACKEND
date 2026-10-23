package com.fitmentor.domain.stats;

import com.fitmentor.domain.session.CompletedSession;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StatsCalculatorTests {

    private static final Instant NOW = Instant.parse("2026-10-21T12:00:00Z");

    @Test
    void emptyHistoryHasZeroTotalsAndNullFreeAverages() {
        StatsCalculator.Stats stats = StatsCalculator.compute(List.of(), NOW, "BEGINNER");

        assertThat(stats.totalSessions()).isZero();
        assertThat(stats.avgAccuracy()).isZero();
        assertThat(stats.streakDays()).isZero();
        assertThat(stats.weekly()).hasSize(7);
        assertThat(stats.weekly()).allSatisfy(day -> assertThat(day.reps()).isZero());
        assertThat(stats.weekly().get(6).date()).isEqualTo("2026-10-21");
        assertThat(stats.weekly().get(0).date()).isEqualTo("2026-10-15");
    }

    @Test
    void aggregatesTotalsAveragesAndPerExercise() {
        List<CompletedSession> sessions = List.of(
            session("squat", 10, 80.0, 20.0, "2026-10-21T08:00:00Z"),
            session("squat", 20, 90.0, 30.0, "2026-10-20T08:00:00Z"),
            session("plank", 5, 70.0, 10.0, "2026-10-21T09:00:00Z")
        );

        StatsCalculator.Stats stats = StatsCalculator.compute(sessions, NOW, "INTERMEDIATE");

        assertThat(stats.totalSessions()).isEqualTo(3);
        assertThat(stats.totalReps()).isEqualTo(35);
        assertThat(stats.totalCalories()).isEqualTo(60.0);
        assertThat(stats.avgAccuracy()).isEqualTo(80.0);
        assertThat(stats.level()).isEqualTo("INTERMEDIATE");
        assertThat(stats.byExercise()).containsExactly(
            new StatsCalculator.ExerciseStat("plank", 5, 70.0),
            new StatsCalculator.ExerciseStat("squat", 30, 85.0));
    }

    @Test
    void streakCountsConsecutiveDaysEndingTodayOrYesterday() {
        List<CompletedSession> sessions = List.of(
            session("squat", 1, 80, 1, "2026-10-21T08:00:00Z"),
            session("squat", 1, 80, 1, "2026-10-20T08:00:00Z"),
            session("squat", 1, 80, 1, "2026-10-19T08:00:00Z"),
            session("squat", 1, 80, 1, "2026-10-17T08:00:00Z")
        );

        assertThat(StatsCalculator.compute(sessions, NOW, "BEGINNER").streakDays()).isEqualTo(3);
    }

    @Test
    void streakSurvivesUntilEndOfYesterdayButBreaksAfterTwoMissedDays() {
        List<CompletedSession> yesterdayOnly = List.of(session("squat", 1, 80, 1, "2026-10-20T23:00:00Z"));
        List<CompletedSession> old = List.of(session("squat", 1, 80, 1, "2026-10-19T08:00:00Z"));

        assertThat(StatsCalculator.compute(yesterdayOnly, NOW, "BEGINNER").streakDays()).isEqualTo(1);
        assertThat(StatsCalculator.compute(old, NOW, "BEGINNER").streakDays()).isZero();
    }

    @Test
    void weeklyChartUsesUtcDaysAndCountsSessionsOfEachDay() {
        List<CompletedSession> sessions = List.of(
            session("squat", 12, 90.0, 1, "2026-10-21T00:30:00Z"),
            session("squat", 8, 70.0, 1, "2026-10-18T10:00:00Z"),
            session("squat", 99, 99.0, 1, "2026-10-01T10:00:00Z")
        );

        List<StatsCalculator.DayStat> weekly = StatsCalculator.compute(sessions, NOW, "BEGINNER").weekly();

        assertThat(weekly.get(6)).isEqualTo(new StatsCalculator.DayStat("2026-10-21", 12, 90.0));
        assertThat(weekly.get(3)).isEqualTo(new StatsCalculator.DayStat("2026-10-18", 8, 70.0));
        assertThat(weekly.stream().mapToInt(StatsCalculator.DayStat::reps).sum()).isEqualTo(20);
    }

    @Test
    void activeDaysCountsDistinctDaysInLastSevenDays() {
        List<CompletedSession> sessions = List.of(
            session("squat", 1, 80, 1, "2026-10-21T08:00:00Z"),
            session("squat", 1, 80, 1, "2026-10-21T20:00:00Z"),
            session("squat", 1, 80, 1, "2026-10-16T08:00:00Z"),
            session("squat", 1, 80, 1, "2026-10-01T08:00:00Z")
        );

        assertThat(StatsCalculator.activeDaysInLastWeek(sessions, NOW)).isEqualTo(2);
    }

    private static CompletedSession session(String exercise, int reps, double accuracy, double calories, String at) {
        return new CompletedSession(exercise, reps, accuracy, calories, 60, Instant.parse(at));
    }
}
