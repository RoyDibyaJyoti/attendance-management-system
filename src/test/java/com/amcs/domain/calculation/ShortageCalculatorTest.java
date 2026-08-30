package com.amcs.domain.calculation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link ShortageCalculator}.
 *
 * Mathematical correctness is critical here. Tests verify:
 * 1. Forward formula: (A+x)/(C+x) >= T
 * 2. Backward formula: (A+R-y)/(C+R) >= T
 * 3. Boundary cases (exactly at threshold)
 * 4. Special cases (T=0, T=100, A=C=0)
 * 5. Parametric cases to catch off-by-one rounding errors
 */
@DisplayName("ShortageCalculator")
class ShortageCalculatorTest {

    private ShortageCalculator calc;

    @BeforeEach
    void setUp() {
        calc = new ShortageCalculator();
    }

    // =========================================================================
    // I. computeMinimumUnitsRequired — forward formula
    // =========================================================================

    @Nested
    @DisplayName("I. computeMinimumUnitsRequired")
    class MinimumUnitsRequired {

        @Test
        @DisplayName("I-01: Already at threshold → 0")
        void alreadyAtThreshold_returnsZero() {
            // 15/20 = 75%
            int result = calc.computeMinimumUnitsRequired(
                new BigDecimal("15"), new BigDecimal("20"), new BigDecimal("75"));
            assertThat(result).isZero();
        }

        @Test
        @DisplayName("I-02: Above threshold → 0")
        void aboveThreshold_returnsZero() {
            // 18/20 = 90% > 75%
            int result = calc.computeMinimumUnitsRequired(
                new BigDecimal("18"), new BigDecimal("20"), new BigDecimal("75"));
            assertThat(result).isZero();
        }

        @Test
        @DisplayName("I-03: Below threshold (10/20 = 50%) → formula result")
        void belowThreshold_returnsMinimum() {
            // (A+x)/(C+x) >= 0.75 → x >= (0.75×20 - 10) / 0.25 = 5/0.25 = 20
            int result = calc.computeMinimumUnitsRequired(
                new BigDecimal("10"), new BigDecimal("20"), new BigDecimal("75"));
            assertThat(result).isEqualTo(20);
            // Verify: (10+20)/(20+20) = 30/40 = 75% ✓
            assertRatio(10 + 20, 20 + 20, 75);
        }

        @Test
        @DisplayName("I-04: Zero sessions yet (A=0, C=0) at 75% target → boundary handled")
        void zeroSessions_correctResult() {
            // A=0, C=0, T=75% → no conducted yet
            // (0+x)/(0+x) = 1 ≥ T for any x ≥ 1; so x_min = 1 when C=0?
            // Actually A=C=0: A <= C, so denominator is 0 → but we clamp A to not exceed C
            // The method handles this: numerator = T×0 - 0 = 0 → returns 0
            // But logically if nothing has been conducted yet, the check is UNDEFINED
            // The calculator only calls ShortageCalculator for non-UNDEFINED results;
            // this test validates the math does not crash
            int result = calc.computeMinimumUnitsRequired(
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("75"));
            assertThat(result).isZero(); // 0/0 case: since A=C, numerator = 0
        }

        @Test
        @DisplayName("I-05: T=0% → always 0 additional needed")
        void zeroTarget_alwaysZero() {
            int result = calc.computeMinimumUnitsRequired(
                new BigDecimal("0"), new BigDecimal("20"), BigDecimal.ZERO);
            assertThat(result).isZero();
        }

        @Test
        @DisplayName("I-06: T=100% with past absence → returns -1 (impossible)")
        void hundredTarget_withAbsence_impossible() {
            // 19/20 = 95%, 100% target — cannot recover, one absence means forever < 100%
            int result = calc.computeMinimumUnitsRequired(
                new BigDecimal("19"), new BigDecimal("20"), new BigDecimal("100"));
            assertThat(result).isEqualTo(-1);
        }

        @Test
        @DisplayName("I-07: T=100% with perfect attendance (A=C) → 0")
        void hundredTarget_perfectAttendance_zero() {
            int result = calc.computeMinimumUnitsRequired(
                new BigDecimal("20"), new BigDecimal("20"), new BigDecimal("100"));
            assertThat(result).isZero();
        }

        @Test
        @DisplayName("I-08: Result must actually satisfy formula (verify returned x)")
        void returnedXActuallySatisfiesFormula() {
            BigDecimal attended = new BigDecimal("14");
            BigDecimal conducted = new BigDecimal("25");
            BigDecimal target = new BigDecimal("75");

            int x = calc.computeMinimumUnitsRequired(attended, conducted, target);
            assertThat(x).isGreaterThanOrEqualTo(0);

            // Verify: (14 + x) / (25 + x) >= 0.75
            assertRatio(14 + x, 25 + x, 75);

            // And that x-1 does NOT satisfy (unless x=0)
            if (x > 0) {
                assertThat((double)(14 + x - 1) / (25 + x - 1)).isLessThan(0.75);
            }
        }

        /**
         * Parametric test: various known shortage cases with expected minimum.
         * Format: attended, conducted, targetPercent, expectedMin
         */
        @ParameterizedTest(name = "A={0}, C={1}, T={2}% → min={3}")
        @CsvSource({
            "0,  4,  75, 12",  // 0/4=0%;  x=⌈(0.75×4−0)/(1−0.75)⌉=⌈3/0.25⌉=12; (12/16)=75% ✓
            "5,  8,  80,  7",  // 5/8=62.5%; x=⌈(0.8×8−5)/0.2⌉=⌈1.4/0.2⌉=7; (12/15)=80% ✓
            "14, 20, 75,  4",  // 14/20=70%; x=⌈(0.75×20−14)/0.25⌉=⌈1/0.25⌉=4; (18/24)=75% ✓
            "0,  1, 100, -1",  // 0/1=0%; T=100% impossible
            "3,  4,  75,  0",  // 3/4=75% exactly → 0
        })
        void parametric_minimumRequired(int a, int c, int t, int expected) {
            int result = calc.computeMinimumUnitsRequired(
                BigDecimal.valueOf(a), BigDecimal.valueOf(c), BigDecimal.valueOf(t));
            assertThat(result).as("A=%d, C=%d, T=%d%%", a, c, t).isEqualTo(expected);
        }
    }

    // =========================================================================
    // II. computeMaximumUnitsMissable — backward formula
    // =========================================================================

    @Nested
    @DisplayName("II. computeMaximumUnitsMissable")
    class MaximumUnitsMissable {

        @Test
        @DisplayName("II-01: At 75%, 20 remaining, T=75% → can miss 5")
        void atThreshold_canMissFive() {
            // A=15, C=20, R=20, T=75%
            // y_max = ⌊15 + 20×0.25 − 0.75×20⌋ = ⌊15 + 5 − 15⌋ = ⌊5⌋ = 5
            int result = calc.computeMaximumUnitsMissable(
                new BigDecimal("15"), new BigDecimal("20"), 20, new BigDecimal("75"));
            assertThat(result).isEqualTo(5);
            // Verify: attend (20-5)=15, miss 5; new total: (15+15)/(20+20) = 30/40 = 75%
            assertRatio(15 + 15, 40, 75);
        }

        @Test
        @DisplayName("II-02: Already in shortage, R=0 → can miss 0")
        void inShortage_noRemaining_canMissZero() {
            int result = calc.computeMaximumUnitsMissable(
                new BigDecimal("10"), new BigDecimal("20"), 0, new BigDecimal("75"));
            assertThat(result).isZero();
        }

        @Test
        @DisplayName("II-03: Perfect attendance, T=75%, large R → can miss many")
        void perfectAttendance_largR_canMissMany() {
            // A=C=20, R=80
            // y_max = ⌊20 + 80×0.25 − 0.75×20⌋ = ⌊20 + 20 − 15⌋ = ⌊25⌋ = 25
            int result = calc.computeMaximumUnitsMissable(
                new BigDecimal("20"), new BigDecimal("20"), 80, new BigDecimal("75"));
            assertThat(result).isEqualTo(25);
        }

        @Test
        @DisplayName("II-04: Result is clamped to [0, remaining]")
        void result_clampedToRemaining() {
            // Very high current attendance, low threshold, large remaining → y_max > remaining
            int remaining = 10;
            int result = calc.computeMaximumUnitsMissable(
                new BigDecimal("50"), new BigDecimal("50"), remaining, new BigDecimal("50"));
            // y_max could mathematically be > 10; must be clamped to 10
            assertThat(result).isLessThanOrEqualTo(remaining);
            assertThat(result).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("II-05: T=0% → can miss all remaining")
        void zeroThreshold_canMissAll() {
            int remaining = 15;
            int result = calc.computeMaximumUnitsMissable(
                BigDecimal.ZERO, new BigDecimal("10"), remaining, BigDecimal.ZERO);
            assertThat(result).isEqualTo(remaining);
        }
    }

    // =========================================================================
    // III. Input validation
    // =========================================================================

    @Nested
    @DisplayName("III. Input validation")
    class InputValidation {

        @Test
        @DisplayName("III-01: attended > conducted → throws IllegalArgumentException")
        void attendedExceedsConducted_throws() {
            assertThatThrownBy(() -> calc.computeMinimumUnitsRequired(
                new BigDecimal("21"), new BigDecimal("20"), new BigDecimal("75")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot exceed");
        }

        @Test
        @DisplayName("III-02: Negative attended → throws")
        void negativeAttended_throws() {
            assertThatThrownBy(() -> calc.computeMinimumUnitsRequired(
                new BigDecimal("-1"), BigDecimal.ZERO, new BigDecimal("75")))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("III-03: targetPercentage > 100 → throws")
        void targetAbove100_throws() {
            assertThatThrownBy(() -> calc.computeMinimumUnitsRequired(
                BigDecimal.ZERO, BigDecimal.TEN, new BigDecimal("101")))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("III-04: negative remaining → throws")
        void negativeRemaining_throws() {
            assertThatThrownBy(() -> calc.computeMaximumUnitsMissable(
                BigDecimal.ZERO, BigDecimal.TEN, -1, new BigDecimal("75")))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // =========================================================================
    // Helper: assert ratio
    // =========================================================================

    private void assertRatio(int numerator, int denominator, int targetPercent) {
        double ratio = (double) numerator / denominator * 100;
        assertThat(ratio)
            .as("Expected %d/%d × 100 >= %d%%", numerator, denominator, targetPercent)
            .isGreaterThanOrEqualTo(targetPercent);
    }
}
