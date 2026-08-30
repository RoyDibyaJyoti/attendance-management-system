package com.amcs.domain.calculation.result;

import com.amcs.domain.academic.AcademicPeriod;
import com.amcs.domain.policy.OverallAggregationStrategy;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Result of the overall/general attendance calculation for a student.
 *
 * <p>IMPORTANT: The {@code strategy} field MUST be read alongside the
 * {@code overallPercentage} to interpret the result correctly. Two results
 * with different strategies are NOT directly comparable even if the numbers
 * coincidentally appear similar.
 *
 * <p>REQUIRES INSTITUTIONAL CONFIRMATION (RIC-OA-002): which strategy is correct,
 * and (RIC-OA-003): what threshold applies to overall attendance.
 */
public record OverallAttendanceResult(
    UUID studentId,
    AcademicPeriod period,
    UUID policyId,
    int policyVersion,
    OverallAggregationStrategy strategy,
    List<SubjectAttendanceResult> subjectResults,
    BigDecimal overallPercentage,
    BigDecimal minimumThresholdPercentage,
    boolean isShortage,
    Instant computedAt
) {
    public OverallAttendanceResult {
        Objects.requireNonNull(studentId, "studentId must not be null");
        Objects.requireNonNull(period, "period must not be null");
        Objects.requireNonNull(policyId, "policyId must not be null");
        Objects.requireNonNull(strategy, "strategy must not be null");
        Objects.requireNonNull(subjectResults, "subjectResults must not be null");
        Objects.requireNonNull(overallPercentage, "overallPercentage must not be null");
        Objects.requireNonNull(minimumThresholdPercentage, "minimumThresholdPercentage must not be null");
        Objects.requireNonNull(computedAt, "computedAt must not be null");
        if (policyVersion < 1) throw new IllegalArgumentException("policyVersion must be >= 1");
        subjectResults = List.copyOf(subjectResults); // defensive copy
    }
}
