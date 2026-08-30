package com.amcs.domain.calculation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Pure mathematical calculator for attendance shortage analysis.
 *
 * <h3>No side effects. No framework dependencies.</h3>
 * <p>This class performs only arithmetic. It has no knowledge of sessions,
 * records, policies, or databases.
 *
 * <h3>Precision</h3>
 * <p>All operations use {@link BigDecimal} to avoid binary floating-point
 * rounding errors. The internal scale is {@link #CALC_SCALE} = 10 decimal places.
 * Results are integers (returned as {@code int}).
 *
 * <h2>Method 1: computeMinimumUnitsRequired</h2>
 * <p>Finds the minimum additional units x (assuming these units are ALL attended
 * and also conducted) such that the new attendance percentage reaches the target:
 * <pre>
 *   (attended + x) / (conducted + x) ≥ target / 100
 * </pre>
 * <p>Derivation:
 * <pre>
 *   attended + x ≥ T × (conducted + x)       where T = target/100
 *   x × (1 − T) ≥ T × conducted − attended
 *   x ≥ (T × conducted − attended) / (1 − T)   [valid when T < 1]
 *   x_min = max(0, ⌈(T × conducted − attended) / (1 − T)⌉)
 * </pre>
 * <p>Special cases:
 * <ul>
 *   <li>T = 0%: always 0 (trivially satisfied).</li>
 *   <li>T = 100%: only possible if attended = conducted (no past absences).
 *       Returns -1 if impossible.</li>
 *   <li>attended ≥ T × conducted: already at or above threshold. Returns 0.</li>
 * </ul>
 *
 * <h2>Method 2: computeMaximumUnitsMissable</h2>
 * <p>Given that {@code remaining} future units will ALL be conducted, finds the
 * maximum number y that the student can be absent for while still reaching the target:
 * <pre>
 *   (attended + remaining − y) / (conducted + remaining) ≥ T
 *   y ≤ attended + remaining × (1 − T) − T × conducted
 *   y_max = ⌊attended + remaining × (1 − T) − T × conducted⌋
 * </pre>
 * <p>Result is clamped to [0, remaining].
 */
public class ShortageCalculator {

    static final int CALC_SCALE = 10;
    static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    static final BigDecimal HUNDRED = new BigDecimal("100");
    static final BigDecimal ONE = BigDecimal.ONE;
    static final BigDecimal ZERO = BigDecimal.ZERO;

    /**
     * Computes the minimum additional units that must be attended (and conducted)
     * to reach the target attendance percentage.
     *
     * @param attended         current attended units (may be fractional due to partial contributions)
     * @param conducted        current conducted units (must be >= attended)
     * @param targetPercentage target threshold percentage in [0, 100]
     * @return minimum additional units to attend; 0 if already at/above target;
     *         -1 if mathematically impossible (100% target with past absences)
     * @throws IllegalArgumentException if any input is out of its valid range
     */
    public int computeMinimumUnitsRequired(
        BigDecimal attended, BigDecimal conducted, BigDecimal targetPercentage
    ) {
        validateAttendanceInputs(attended, conducted, targetPercentage);

        // T = target as fraction [0, 1]
        BigDecimal T = targetPercentage.divide(HUNDRED, CALC_SCALE + 4, ROUNDING);

        // T = 0%: trivially satisfied with 0 additional sessions
        if (T.compareTo(ZERO) == 0) return 0;

        // Check if already at or above target (denominator > 0 required for meaningful check)
        if (conducted.compareTo(ZERO) > 0) {
            BigDecimal currentPercent = attended.multiply(HUNDRED)
                .divide(conducted, CALC_SCALE, ROUNDING);
            if (currentPercent.compareTo(targetPercentage) >= 0) return 0;
        }

        // T = 100% special case: (A+x)/(C+x) < 1 whenever A < C, for all finite x
        if (targetPercentage.compareTo(new BigDecimal("100")) == 0) {
            // Only reachable if attended == conducted (no past absences at all)
            if (attended.compareTo(conducted) >= 0) return 0;
            return -1; // impossible
        }

        // Standard case: T ∈ (0, 1) exclusive
        // x_min = ⌈(T × C − A) / (1 − T)⌉
        BigDecimal oneMinusT = ONE.subtract(T);
        BigDecimal numerator = T.multiply(conducted).subtract(attended);

        if (numerator.compareTo(ZERO) <= 0) {
            // Already at or above threshold (edge: numerator exactly 0 means exactly at threshold)
            return 0;
        }

        // Compute ceiling using exact BigDecimal arithmetic
        // xMinExact = numerator / (1 - T)
        BigDecimal xMinExact = numerator.divide(oneMinusT, CALC_SCALE, ROUNDING);

        // Convert to ceiling integer safely
        long xMin = ceiling(xMinExact);

        // Verify with exact BigDecimal arithmetic (defends against rounding errors at the boundary)
        xMin = verifyAndAdjustCeiling(attended, conducted, T, xMin);

        return (int) Math.max(0, xMin);
    }

    /**
     * Computes the maximum number of additional units a student can be absent for
     * (out of {@code remaining} future sessions, all of which will be conducted)
     * while still reaching the target percentage.
     *
     * @param attended         current attended units
     * @param conducted        current conducted units
     * @param remaining        number of future units that will be conducted (>= 0)
     * @param targetPercentage target threshold percentage in [0, 100]
     * @return maximum units that can be missed [0, remaining];
     *         0 if the student must attend all remaining and may still fall short
     */
    public int computeMaximumUnitsMissable(
        BigDecimal attended, BigDecimal conducted, int remaining, BigDecimal targetPercentage
    ) {
        validateAttendanceInputs(attended, conducted, targetPercentage);
        if (remaining < 0) throw new IllegalArgumentException("remaining must not be negative, got: " + remaining);

        BigDecimal T = targetPercentage.divide(HUNDRED, CALC_SCALE + 4, ROUNDING);
        BigDecimal R = BigDecimal.valueOf(remaining);
        BigDecimal oneMinusT = ONE.subtract(T);

        // y_max = ⌊attended + R × (1 − T) − T × conducted⌋
        BigDecimal yMaxExact = attended
            .add(R.multiply(oneMinusT))
            .subtract(T.multiply(conducted));

        if (yMaxExact.compareTo(ZERO) < 0) {
            // Cannot miss any sessions; student may still fall short of target
            return 0;
        }

        // Floor the result
        long yMax = floor(yMaxExact);

        // Clamp to [0, remaining]
        yMax = Math.max(0, Math.min(yMax, remaining));
        return (int) yMax;
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Returns the ceiling of a BigDecimal value as a long.
     * Uses setScale with CEILING rounding mode for correctness.
     */
    private long ceiling(BigDecimal value) {
        return value.setScale(0, RoundingMode.CEILING).longValueExact();
    }

    /**
     * Returns the floor of a BigDecimal value as a long.
     */
    private long floor(BigDecimal value) {
        return value.setScale(0, RoundingMode.FLOOR).longValueExact();
    }

    /**
     * Verifies that {@code candidate} satisfies the target formula and adjusts by ±1 if needed.
     * Guards against rare edge cases where double arithmetic in ceiling() may be off by 1.
     */
    private long verifyAndAdjustCeiling(
        BigDecimal attended, BigDecimal conducted, BigDecimal T, long candidate) {

        // Check if (candidate - 1) works (we may have over-estimated by 1)
        if (candidate > 0 && satisfiesTarget(attended, conducted, T, candidate - 1)) {
            return candidate - 1;
        }
        // Check if candidate works; if not, increment until it does
        int guard = 0;
        while (!satisfiesTarget(attended, conducted, T, candidate)) {
            candidate++;
            if (++guard > 10_000) {
                throw new IllegalStateException(
                    "ShortageCalculator: ceiling verification exceeded safety limit. "
                        + "Inputs: attended=" + attended + ", conducted=" + conducted + ", T=" + T);
            }
        }
        return candidate;
    }

    /** Returns true if adding x attended+conducted units satisfies the target. */
    private boolean satisfiesTarget(BigDecimal attended, BigDecimal conducted, BigDecimal T, long x) {
        if (x < 0) return false;
        BigDecimal xBD = BigDecimal.valueOf(x);
        BigDecimal newAttended = attended.add(xBD);
        BigDecimal newConducted = conducted.add(xBD);
        if (newConducted.compareTo(ZERO) == 0) return false;
        BigDecimal ratio = newAttended.divide(newConducted, CALC_SCALE + 4, ROUNDING);
        return ratio.compareTo(T) >= 0;
    }

    private void validateAttendanceInputs(
        BigDecimal attended, BigDecimal conducted, BigDecimal targetPercentage) {
        Objects.requireNonNull(attended, "attended must not be null");
        Objects.requireNonNull(conducted, "conducted must not be null");
        Objects.requireNonNull(targetPercentage, "targetPercentage must not be null");

        if (attended.compareTo(ZERO) < 0)
            throw new IllegalArgumentException("attended must not be negative, got: " + attended);
        if (conducted.compareTo(ZERO) < 0)
            throw new IllegalArgumentException("conducted must not be negative, got: " + conducted);
        if (attended.compareTo(conducted) > 0)
            throw new IllegalArgumentException(
                "attended [%s] cannot exceed conducted [%s]".formatted(attended, conducted));
        if (targetPercentage.compareTo(ZERO) < 0 || targetPercentage.compareTo(HUNDRED) > 0)
            throw new IllegalArgumentException(
                "targetPercentage must be in [0, 100], got: " + targetPercentage);
    }
}
