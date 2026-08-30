package com.amcs.domain.calculation.result;

import com.amcs.domain.academic.AcademicPeriod;
import com.amcs.domain.attendance.AttendanceClassification;
import com.amcs.domain.policy.MissingRecordStrategy;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable result of a subject-level attendance calculation for a single student.
 *
 * <h3>Source-of-Truth Principle</h3>
 * <p>The {@code attendancePercentage} is DERIVED from {@code attendedUnits} and
 * {@code conductedUnits}. It is computed by the engine, not entered by a user.
 *
 * <h3>Missing Record Traceability</h3>
 * <p>Captures {@code missingRecordStrategy} and {@code missingRecordCount} to ensure
 * total transparency on how unrecorded sessions influenced the calculation.
 */
public record SubjectAttendanceResult(
    UUID studentId,
    UUID subjectId,
    UUID policyId,
    int policyVersion,
    AcademicPeriod period,
    BigDecimal conductedUnits,
    BigDecimal attendedUnits,
    BigDecimal attendancePercentage,
    BigDecimal minimumThresholdPercentage,
    AttendanceClassification classification,
    BigDecimal shortageUnits,
    BigDecimal surplusUnits,
    MissingRecordStrategy missingRecordStrategy,
    int missingRecordCount,
    Instant computedAt
) {
    /** Scale used for percentage display (e.g., 83.33). */
    public static final int PERCENTAGE_SCALE = 2;

    public SubjectAttendanceResult {
        Objects.requireNonNull(studentId, "studentId must not be null");
        Objects.requireNonNull(subjectId, "subjectId must not be null");
        Objects.requireNonNull(policyId, "policyId must not be null");
        Objects.requireNonNull(period, "period must not be null");
        Objects.requireNonNull(conductedUnits, "conductedUnits must not be null");
        Objects.requireNonNull(attendedUnits, "attendedUnits must not be null");
        Objects.requireNonNull(attendancePercentage, "attendancePercentage must not be null");
        Objects.requireNonNull(minimumThresholdPercentage, "minimumThresholdPercentage must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(shortageUnits, "shortageUnits must not be null");
        Objects.requireNonNull(surplusUnits, "surplusUnits must not be null");
        Objects.requireNonNull(missingRecordStrategy, "missingRecordStrategy must not be null");
        Objects.requireNonNull(computedAt, "computedAt must not be null");
        if (missingRecordCount < 0) {
            throw new IllegalArgumentException("missingRecordCount must not be negative, got: " + missingRecordCount);
        }
    }

    /**
     * Backward-compatible constructor defaulting missing record tracking.
     */
    public SubjectAttendanceResult(
        UUID studentId,
        UUID subjectId,
        UUID policyId,
        int policyVersion,
        AcademicPeriod period,
        BigDecimal conductedUnits,
        BigDecimal attendedUnits,
        BigDecimal attendancePercentage,
        BigDecimal minimumThresholdPercentage,
        AttendanceClassification classification,
        BigDecimal shortageUnits,
        BigDecimal surplusUnits,
        Instant computedAt
    ) {
        this(studentId, subjectId, policyId, policyVersion, period,
            conductedUnits, attendedUnits, attendancePercentage, minimumThresholdPercentage,
            classification, shortageUnits, surplusUnits,
            MissingRecordStrategy.TREAT_AS_ABSENT, 0, computedAt);
    }

    public boolean isShortage() {
        return classification == AttendanceClassification.SHORTAGE;
    }

    public boolean isAdequate() {
        return classification == AttendanceClassification.ADEQUATE;
    }

    public boolean isUndefined() {
        return classification == AttendanceClassification.UNDEFINED;
    }

    public boolean isIncomplete() {
        return classification == AttendanceClassification.INCOMPLETE;
    }
}
