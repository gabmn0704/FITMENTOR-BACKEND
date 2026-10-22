package com.fitmentor.domain.routine;

import com.fitmentor.domain.user.FitnessLevel;

/**
 * Input of the routine algorithm.
 *
 * @param level            the user's declared level.
 * @param avgAccuracy      average accuracy of recent sessions, or null when there is no history.
 * @param activeDaysInWeek distinct training days in the last seven days.
 */
public record RoutineContext(FitnessLevel level, Double avgAccuracy, int activeDaysInWeek) {

    public boolean hasHistory() {
        return avgAccuracy != null;
    }
}
