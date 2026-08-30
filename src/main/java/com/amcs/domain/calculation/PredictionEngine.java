package com.amcs.domain.calculation;

import com.amcs.domain.calculation.result.PredictionResult;
import com.amcs.domain.calculation.result.SubjectAttendanceResult;
import com.amcs.domain.attendance.AttendanceClassification;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Computes future attendance predictions for a student in a subject.
 *
 * <p>Uses {@link ShortageCalculator} for the mathematical formulas.
 *
 * <h3>The two prediction questions answered</h3>
 *
 * <p><b>Question 1 — minimumUnitsToAttend</b>:
 * "Starting from now, if you attend x MORE sessions (and ONLY those sessions are
 * conducted beyond what already occurred), what is the minimum x to reach the threshold?"
 * Formula: (A + x) / (C + x) ≥ T → x_min = ⌈(T×C − A) / (1 − T)⌉
 *
 * <p><b>Question 2 — maximumUnitsCanMiss</b>:
 * "Of the known R remaining sessions (ALL of which will be conducted), how many
 * can you afford to miss while still reaching the threshold?"
 * Formula: (A + R − y) / (C + R) ≥ T → y_max = ⌊A + R×(1−T) − T×C⌋
 *
 * <p><b>canReachTarget</b>: True if minimumUnitsToAttend ≤ remainingUnits.
 * (Using Question 2's formula: (A + R) / (C + R) ≥ T — attend everything remaining.)
 *
 * <h3>UNDEFINED base case</h3>
 * <p>If no sessions have been conducted yet (classification = UNDEFINED),
 * the student starts from a clean slate. To reach threshold T with R remaining units:
 * x_min = ⌈T × R / 100⌉
 *
 * <h3>DISCLAIMER</h3>
 * <p>All predictions depend on an accurate count of remaining sessions.
 * This count REQUIRES INSTITUTIONAL CONFIRMATION (RIC-FP-005).
 */
public class PredictionEngine {

    private final ShortageCalculator shortageCalculator;

    public PredictionEngine(ShortageCalculator shortageCalculator) {
        this.shortageCalculator = Objects.requireNonNull(shortageCalculator, "shortageCalculator");
    }

    /**
     * Computes a prediction result.
     *
     * @param studentId      the student
     * @param subjectId      the subject
     * @param currentResult  current subject attendance result
     * @param remainingUnits expected remaining units to be conducted (>= 0)
     * @return an immutable {@link PredictionResult}
     */
    public PredictionResult predict(
        UUID studentId,
        UUID subjectId,
        SubjectAttendanceResult currentResult,
        int remainingUnits
    ) {
        Objects.requireNonNull(studentId, "studentId must not be null");
        Objects.requireNonNull(subjectId, "subjectId must not be null");
        Objects.requireNonNull(currentResult, "currentResult must not be null");
        if (remainingUnits < 0)
            throw new IllegalArgumentException("remainingUnits must not be negative, got: " + remainingUnits);

        BigDecimal attended = currentResult.attendedUnits();
        BigDecimal conducted = currentResult.conductedUnits();
        BigDecimal target = currentResult.minimumThresholdPercentage();

        boolean isAlreadyAbove = currentResult.isAdequate();

        // ── UNDEFINED base case: no sessions conducted yet ──────────────────
        if (currentResult.isUndefined()) {
            int minToAttend = computeRequiredFromZero(target, remainingUnits);
            int maxToMiss = Math.max(0, remainingUnits - minToAttend);
            boolean canReach = minToAttend <= remainingUnits;

            return new PredictionResult(
                studentId, subjectId,
                BigDecimal.ZERO, target,
                BigDecimal.ZERO, BigDecimal.ZERO,
                remainingUnits, canReach, false,
                minToAttend, maxToMiss,
                Instant.now()
            );
        }

        // ── Question 1: minimum x for (A+x)/(C+x) ≥ T ──────────────────────
        int minimumRequired = shortageCalculator.computeMinimumUnitsRequired(attended, conducted, target);

        // ── Question 2: can student reach target with perfect attendance? ───
        // Check: if student attends ALL remaining, does (A+R)/(C+R) ≥ T?
        boolean canReachTarget;
        if (minimumRequired == -1) {
            // 100% threshold is mathematically impossible (past absences exist)
            canReachTarget = false;
        } else if (isAlreadyAbove) {
            canReachTarget = true;
        } else {
            // canReach = true if attending all remaining would bring student to threshold
            // i.e., minimumRequired ≤ remainingUnits
            canReachTarget = minimumRequired <= remainingUnits;
        }

        // ── minimumUnitsToAttend ─────────────────────────────────────────────
        int minToAttend;
        if (isAlreadyAbove) {
            minToAttend = 0;
        } else if (minimumRequired == -1 || !canReachTarget) {
            // Impossible or can't make it — show remainingUnits+1 to signal "need more than available"
            minToAttend = remainingUnits + 1;
        } else {
            minToAttend = minimumRequired;
        }

        // ── maximumUnitsCanMiss ──────────────────────────────────────────────
        int maxToMiss;
        if (!canReachTarget) {
            maxToMiss = 0;
        } else {
            maxToMiss = shortageCalculator.computeMaximumUnitsMissable(
                attended, conducted, remainingUnits, target);
        }

        return new PredictionResult(
            studentId, subjectId,
            currentResult.attendancePercentage(), target,
            attended, conducted,
            remainingUnits, canReachTarget, isAlreadyAbove,
            Math.max(0, minToAttend),
            Math.max(0, maxToMiss),
            Instant.now()
        );
    }

    /**
     * For the UNDEFINED base case (0 conducted sessions), computes the minimum
     * units to attend from the remaining sessions to reach the target.
     *
     * <p>Formula: x / remainingUnits ≥ T/100  →  x_min = ⌈T × remainingUnits / 100⌉
     */
    private int computeRequiredFromZero(BigDecimal targetPercentage, int remaining) {
        if (remaining == 0) return 0;
        BigDecimal T = targetPercentage.divide(new BigDecimal("100"), ShortageCalculator.CALC_SCALE + 4, RoundingMode.HALF_UP);
        BigDecimal needed = T.multiply(BigDecimal.valueOf(remaining));
        long ceil = needed.setScale(0, RoundingMode.CEILING).longValueExact();
        return (int) Math.min(ceil, remaining);
    }
}
