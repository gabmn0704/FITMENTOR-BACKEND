package com.fitmentor.domain.stats;

import com.fitmentor.domain.session.CompletedSession;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Pure aggregation of a user's sessions. Days are UTC, as in the frontend contract.
 * The streak counts consecutive UTC days with at least one session, ending today or yesterday.
 */
public final class StatsCalculator {

    private StatsCalculator() {
    }

    public static Stats compute(List<CompletedSession> sessions, Instant now, String level) {
        LocalDate today = day(now);
        int totalReps = sessions.stream().mapToInt(CompletedSession::reps).sum();
        double totalCalories = round(sessions.stream().mapToDouble(CompletedSession::calories).sum());
        return new Stats(
            sessions.size(),
            totalReps,
            totalCalories,
            average(sessions),
            streakDays(sessions, today),
            weekly(sessions, today),
            byExercise(sessions),
            level
        );
    }

    /** Average accuracy of the given sessions, 0 when there are none. */
    public static double average(List<CompletedSession> sessions) {
        if (sessions.isEmpty()) {
            return 0.0;
        }
        return round(sessions.stream().mapToDouble(CompletedSession::accuracy).average().orElse(0.0));
    }

    /** Distinct UTC training days within the seven days ending today. */
    public static int activeDaysInLastWeek(List<CompletedSession> sessions, Instant now) {
        LocalDate today = day(now);
        return (int) sessions.stream()
            .map(session -> day(session.createdAt()))
            .filter(date -> !date.isBefore(today.minusDays(6)) && !date.isAfter(today))
            .distinct()
            .count();
    }

    static int streakDays(List<CompletedSession> sessions, LocalDate today) {
        Set<LocalDate> days = sessions.stream().map(session -> day(session.createdAt())).collect(Collectors.toSet());
        LocalDate cursor = days.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (days.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    static List<DayStat> weekly(List<CompletedSession> sessions, LocalDate today) {
        List<DayStat> result = new ArrayList<>();
        for (int offset = 6; offset >= 0; offset--) {
            LocalDate date = today.minusDays(offset);
            List<CompletedSession> dayItems = sessions.stream()
                .filter(session -> day(session.createdAt()).equals(date))
                .toList();
            result.add(new DayStat(date.toString(), dayItems.stream().mapToInt(CompletedSession::reps).sum(), average(dayItems)));
        }
        return result;
    }

    static List<ExerciseStat> byExercise(List<CompletedSession> sessions) {
        Map<String, List<CompletedSession>> grouped = new TreeMap<>();
        for (CompletedSession session : sessions) {
            grouped.computeIfAbsent(session.exercise(), key -> new ArrayList<>()).add(session);
        }
        return grouped.entrySet().stream()
            .map(entry -> new ExerciseStat(
                entry.getKey(),
                entry.getValue().stream().mapToInt(CompletedSession::reps).sum(),
                average(entry.getValue())))
            .sorted(Comparator.comparing(ExerciseStat::exercise))
            .toList();
    }

    static LocalDate day(Instant instant) {
        return instant.atZone(ZoneOffset.UTC).toLocalDate();
    }

    static double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /** Aggregated view returned by {@code GET /api/stats}. */
    public record Stats(
        int totalSessions,
        int totalReps,
        double totalCalories,
        double avgAccuracy,
        int streakDays,
        List<DayStat> weekly,
        List<ExerciseStat> byExercise,
        String level
    ) {
    }

    /** One day of the weekly chart. */
    public record DayStat(String date, int reps, double accuracy) {
    }

    /** Per-exercise totals. */
    public record ExerciseStat(String exercise, int reps, double accuracy) {
    }
}
