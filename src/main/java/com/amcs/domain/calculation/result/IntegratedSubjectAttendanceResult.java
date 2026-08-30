package com.amcs.domain.calculation.result;

import com.amcs.domain.academic.AcademicPeriod;
import com.amcs.domain.policy.IntegratedCompositeStrategy;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Result of attendance calculation for a
 * {@link com.amcs.domain.academic.CourseType#THEORY_INTEGRATED_LABORATORY} subject.
 *
 * <p>Contains individual component results for both theory and lab streams,
 * plus the composite result computed according to the configured strategy.
 *
 * <h3>Strategy-specific fields</h3>
 * <ul>
 *   <li>SEPARATE_THRESHOLDS: {@code theoryComponent} and {@code labComponent} are the
 *       authoritative results. {@code combinedResult}, {@code compositePercentage},
 *       and {@code compositeThreshold} are empty.</li>
 *   <li>COMBINED_ALL_SESSIONS: {@code combinedResult} is present (all sessions pooled).
 *       The component results are provided for transparency/display only.</li>
 *   <li>WEIGHTED_AVERAGE: {@code compositePercentage} and {@code compositeThreshold} are present.
 *       The component results are used to compute the weighted composite.</li>
 * </ul>
 *
 * <p>REQUIRES INSTITUTIONAL CONFIRMATION: Which strategy applies?
 * See DOMAIN_RULES.md Section 6, RIC-TL-002.
 */
public record IntegratedSubjectAttendanceResult(
    UUID studentId,
    UUID subjectId,
    AcademicPeriod period,
    IntegratedCompositeStrategy strategy,
    ComponentAttendanceResult theoryComponent,
    ComponentAttendanceResult labComponent,
    Optional<SubjectAttendanceResult> combinedResult,
    Optional<BigDecimal> compositePercentage,
    Optional<BigDecimal> compositeThreshold,
    boolean isShortage,
    Instant computedAt
) {
    public IntegratedSubjectAttendanceResult {
        Objects.requireNonNull(studentId, "studentId must not be null");
        Objects.requireNonNull(subjectId, "subjectId must not be null");
        Objects.requireNonNull(period, "period must not be null");
        Objects.requireNonNull(strategy, "strategy must not be null");
        Objects.requireNonNull(theoryComponent, "theoryComponent must not be null");
        Objects.requireNonNull(labComponent, "labComponent must not be null");
        Objects.requireNonNull(combinedResult, "combinedResult must not be null");
        Objects.requireNonNull(compositePercentage, "compositePercentage must not be null");
        Objects.requireNonNull(compositeThreshold, "compositeThreshold must not be null");
        Objects.requireNonNull(computedAt, "computedAt must not be null");
    }
}
