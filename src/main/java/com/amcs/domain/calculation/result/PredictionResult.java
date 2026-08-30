package com.amcs.domain.calculation.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Result of a future attendance prediction for a student in a subject.
 *
 * <p><strong>DISCLAIMER:</strong> All predictions are estimates. They assume a known,
 * fixed number of remaining sessions. The source of the remaining count REQUIRES
 * INSTITUTIONAL CONFIRMATION (RIC-FP-005). The UI MUST label prediction results
 * as estimates, not guaranteed outcomes.
 *
 * <h3>Two separate prediction metrics</h3>
 *
 * <p><b>minimumUnitsToAttend</b>: Uses formula (A+x)/(C+x) ≥ T.
 * Answers: "If you attend x more sessions (and these are the only future sessions
 * — no more sessions are conducted beyond those), how many must you attend?"
 * This is the mathematically minimum x to hit the threshold.
 *
 * <p><b>maximumUnitsCanMiss</b>: Uses formula (A+R-y)/(C+R) ≥ T, where R is
 * the total remaining sessions. Answers: "Of the known R remaining sessions
 * (all of which will be conducted), how many can you afford to miss?"
 *
 * <h3>Units, not sessions</h3>
 * <p>All counts are in terms of "units" (matching Session.conductedUnits model).
 * If 1 session = 1 unit, the values represent sessions.
 * If 1 lab session = 3 units, the values are in units, not sessions.
 * Conversion to sessions is a display concern for the UI layer.
 *
 * <h3>Invariants</h3>
 * <ul>
 *   <li>{@code minimumUnitsToAttend >= 0}</li>
 *   <li>{@code maximumUnitsCanMiss >= 0}</li>
 *   <li>{@code remainingUnits >= 0}</li>
 *   <li>If {@code isAlreadyAboveTarget}: {@code minimumUnitsToAttend == 0}</li>
 *   <li>If {@code !canReachTarget}: {@code minimumUnitsToAttend > remainingUnits}</li>
 * </ul>
 */
public record PredictionResult(
    UUID studentId,
    UUID subjectId,
    BigDecimal currentPercentage,
    BigDecimal targetPercentage,
    BigDecimal attendedUnits,
    BigDecimal conductedUnits,
    int remainingUnits,
    boolean canReachTarget,
    boolean isAlreadyAboveTarget,
    int minimumUnitsToAttend,
    int maximumUnitsCanMiss,
    Instant computedAt
) {
    public PredictionResult {
        Objects.requireNonNull(studentId, "studentId must not be null");
        Objects.requireNonNull(subjectId, "subjectId must not be null");
        Objects.requireNonNull(currentPercentage, "currentPercentage must not be null");
        Objects.requireNonNull(targetPercentage, "targetPercentage must not be null");
        Objects.requireNonNull(attendedUnits, "attendedUnits must not be null");
        Objects.requireNonNull(conductedUnits, "conductedUnits must not be null");
        Objects.requireNonNull(computedAt, "computedAt must not be null");
        if (remainingUnits < 0)
            throw new IllegalArgumentException("remainingUnits must not be negative, got: " + remainingUnits);
        if (minimumUnitsToAttend < 0)
            throw new IllegalArgumentException("minimumUnitsToAttend must not be negative, got: " + minimumUnitsToAttend);
        if (maximumUnitsCanMiss < 0)
            throw new IllegalArgumentException("maximumUnitsCanMiss must not be negative, got: " + maximumUnitsCanMiss);
    }
}
