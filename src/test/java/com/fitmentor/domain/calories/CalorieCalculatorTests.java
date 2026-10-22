package com.fitmentor.domain.calories;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalorieCalculatorTests {

    @Test
    void appliesMetTimesSeventyKilogramsTimesHours() {
        // 5.0 MET * 70 kg * 1 h = 350 kcal
        assertThat(CalorieCalculator.calories(5.0, 3600)).isEqualTo(350.0);
    }

    @Test
    void roundsToTwoDecimals() {
        // 8.0 * 70 * (100/3600) = 15.5555... -> 15.56
        assertThat(CalorieCalculator.calories(8.0, 100)).isEqualTo(15.56);
    }

    @Test
    void zeroDurationBurnsNothing() {
        assertThat(CalorieCalculator.calories(8.0, 0)).isZero();
    }

    @Test
    void rejectsNegativeInput() {
        assertThatThrownBy(() -> CalorieCalculator.calories(-1, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CalorieCalculator.calories(5, -10)).isInstanceOf(IllegalArgumentException.class);
    }
}
