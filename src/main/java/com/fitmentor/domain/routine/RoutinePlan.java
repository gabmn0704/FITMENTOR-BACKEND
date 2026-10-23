package com.fitmentor.domain.routine;

import java.util.List;

/** Adaptive routine returned by {@code GET /api/routine}. */
public record RoutinePlan(String level, int restSeconds, List<RoutineItem> items, String reason) {

    /** One exercise of the routine. */
    public record RoutineItem(String exercise, int sets, int reps, String note) {
    }
}
