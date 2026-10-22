package com.fitmentor.domain.calories;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Estimates energy expenditure with the MET formula: kcal = MET x weight(kg) x hours.
 * The body weight is fixed at 70 kg in v2, as agreed with the frontend contract.
 */
public final class CalorieCalculator {

    public static final double REFERENCE_WEIGHT_KG = 70.0;

    private CalorieCalculator() {
    }

    public static double calories(double met, int durationSeconds) {
        if (met < 0 || durationSeconds < 0) {
            throw new IllegalArgumentException("MET and duration must not be negative");
        }
        double hours = durationSeconds / 3600.0;
        return BigDecimal.valueOf(met * REFERENCE_WEIGHT_KG * hours)
            .setScale(2, RoundingMode.HALF_UP)
            .doubleValue();
    }
}
