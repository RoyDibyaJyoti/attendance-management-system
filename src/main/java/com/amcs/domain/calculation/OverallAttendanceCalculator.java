package com.amcs.domain.calculation;

import com.amcs.domain.academic.AcademicPeriod;
import com.amcs.domain.attendance.AttendanceClassification;
import com.amcs.domain.calculation.result.OverallAttendanceResult;
import com.amcs.domain.calculation.result.SubjectAttendanceResult;
import com.amcs.domain.policy.OverallAttendancePolicy;
import com.amcs.domain.policy.OverallAggregationStrategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Computes overall/general attendance by aggregating subject-level results
 * using the configured {@link OverallAggregationStrategy}.
 *
 * <h3>The Four Mathematically Distinct Strategies</h3>
 * <ul>
 *   <li><b>{@link OverallAggregationStrategy#ARITHMETIC_MEAN}</b>:
 *       $$\frac{1}{N} \sum_{i=1}^N P_i$$
 *       Unweighted average of percentages. Ignores subject size and credit hours.</li>
 *
 *   <li><b>{@link OverallAggregationStrategy#WEIGHTED_BY_CREDITS}</b>:
 *       $$\frac{\sum (Credits_i \times P_i)}{\sum Credits_i}$$
 *       Weights subject percentages by institutional credit hours.</li>
 *
 *   <li><b>{@link OverallAggregationStrategy#AGGREGATE_UNITS}</b>:
 *       $$\frac{\sum AttendedUnits_i}{\sum ConductedUnits_i} \times 100$$
 *       Raw attendance ratio across all discrete units.</li>
 *
 *   <li><b>{@link OverallAggregationStrategy#AGGREGATE_HOURS}</b>:
 *       $$\frac{\sum (AttendedUnits_i \times HoursPerUnit_i)}{\sum (ConductedUnits_i \times HoursPerUnit_i)} \times 100$$
 *       Converts units into clock/contact hours before ratio calculation.
 *       Mathematically diverges from AGGREGATE_UNITS when courses have differing contact hours per unit
 *       (e.g., 1-hour theory units vs. 2- or 3-hour lab units).</li>
 * </ul>
 *
 * <h3>Integrated Subjects in Overall Attendance (RIC-OA-004)</h3>
 * <p>REQUIRES INSTITUTIONAL CONFIRMATION: Institutions choose between:
 * <ol>
 *   <li><b>Component-level entry:</b> The Theory and Lab components enter as two distinct
 *       results in {@code subjectResults}.</li>
 *   <li><b>Consolidated entry:</b> The integrated course enters as a single combined or
 *       weighted result in {@code subjectResults}.</li>
 * </ol>
 * Both patterns are supported by passing the corresponding {@link SubjectAttendanceResult} instances.
 */
public class OverallAttendanceCalculator {

    private static final int DISPLAY_SCALE = 2;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /**
     * Computes overall attendance with default 1-hour unit equivalence.
     */
    public OverallAttendanceResult calculate(
        UUID studentId,
        List<SubjectAttendanceResult> subjectResults,
        Map<UUID, Integer> creditHoursById,
        OverallAttendancePolicy policy,
        AcademicPeriod period
    ) {
        return calculate(studentId, subjectResults, creditHoursById, Map.of(), policy, period);
    }

    /**
     * Computes overall attendance with explicit contact hours per unit mapping.
     *
     * @param studentId the student
     * @param subjectResults subject-level results
     * @param creditHoursById credit hours by subject (for WEIGHTED_BY_CREDITS)
     * @param hoursPerUnitBySubject contact hours per unit by subject (for AGGREGATE_HOURS; defaults to 1.0)
     * @param policy overall attendance policy
     * @param period academic period
     * @return immutable {@link OverallAttendanceResult}
     */
    public OverallAttendanceResult calculate(
        UUID studentId,
        List<SubjectAttendanceResult> subjectResults,
        Map<UUID, Integer> creditHoursById,
        Map<UUID, BigDecimal> hoursPerUnitBySubject,
        OverallAttendancePolicy policy,
        AcademicPeriod period
    ) {
        Objects.requireNonNull(studentId, "studentId must not be null");
        Objects.requireNonNull(subjectResults, "subjectResults must not be null");
        Objects.requireNonNull(creditHoursById, "creditHoursById must not be null");
        Objects.requireNonNull(hoursPerUnitBySubject, "hoursPerUnitBySubject must not be null");
        Objects.requireNonNull(policy, "policy must not be null");
        Objects.requireNonNull(period, "period must not be null");

        // Exclude UNDEFINED subjects from aggregation
        List<SubjectAttendanceResult> eligible = subjectResults.stream()
            .filter(r -> r.classification() != AttendanceClassification.UNDEFINED)
            .toList();

        BigDecimal overall;

        if (eligible.isEmpty()) {
            overall = BigDecimal.ZERO;
        } else {
            overall = switch (policy.aggregationStrategy()) {
                case ARITHMETIC_MEAN -> computeArithmeticMean(eligible);
                case AGGREGATE_UNITS -> computeAggregateUnits(eligible);
                case AGGREGATE_HOURS -> computeAggregateHours(eligible, hoursPerUnitBySubject);
                case WEIGHTED_BY_CREDITS -> computeWeightedByCredits(eligible, creditHoursById);
            };
        }

        BigDecimal threshold = policy.minimumThresholdPercentage();
        boolean shortage = overall.compareTo(threshold) < 0;

        return new OverallAttendanceResult(
            studentId, period, policy.id(), policy.version(),
            policy.aggregationStrategy(),
            subjectResults,
            overall,
            threshold,
            shortage,
            Instant.now()
        );
    }

    // =========================================================================
    // Strategy implementations
    // =========================================================================

    /** Strategy 1: Σ(percentage) / count */
    private BigDecimal computeArithmeticMean(List<SubjectAttendanceResult> results) {
        BigDecimal sum = results.stream()
            .map(SubjectAttendanceResult::attendancePercentage)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(results.size()), DISPLAY_SCALE, ROUNDING);
    }

    /** Strategy 2: Σ(credits × percentage) / Σ(credits) */
    private BigDecimal computeWeightedByCredits(
        List<SubjectAttendanceResult> results,
        Map<UUID, Integer> creditHoursById
    ) {
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (SubjectAttendanceResult result : results) {
            int credits = creditHoursById.getOrDefault(result.subjectId(), 0);
            if (credits <= 0) continue; // Skip subjects with no credit hours
            BigDecimal creditsBD = BigDecimal.valueOf(credits);
            weightedSum = weightedSum.add(creditsBD.multiply(result.attendancePercentage()));
            totalCredits = totalCredits.add(creditsBD);
        }

        if (totalCredits.compareTo(BigDecimal.ZERO) == 0) {
            return computeArithmeticMean(results);
        }

        return weightedSum.divide(totalCredits, DISPLAY_SCALE, ROUNDING);
    }

    /** Strategy 3: Σ(attendedUnits) / Σ(conductedUnits) × 100 */
    private BigDecimal computeAggregateUnits(List<SubjectAttendanceResult> results) {
        BigDecimal totalAttended = results.stream()
            .map(SubjectAttendanceResult::attendedUnits)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalConducted = results.stream()
            .map(SubjectAttendanceResult::conductedUnits)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalConducted.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        return totalAttended
            .multiply(HUNDRED)
            .divide(totalConducted, DISPLAY_SCALE, ROUNDING);
    }

    /** Strategy 4: Σ(attendedUnits × hours) / Σ(conductedUnits × hours) × 100 */
    private BigDecimal computeAggregateHours(
        List<SubjectAttendanceResult> results,
        Map<UUID, BigDecimal> hoursPerUnitBySubject
    ) {
        BigDecimal totalAttendedHours = BigDecimal.ZERO;
        BigDecimal totalConductedHours = BigDecimal.ZERO;

        for (SubjectAttendanceResult result : results) {
            BigDecimal hours = hoursPerUnitBySubject.getOrDefault(result.subjectId(), BigDecimal.ONE);
            if (hours.compareTo(BigDecimal.ZERO) <= 0) {
                hours = BigDecimal.ONE;
            }
            totalAttendedHours = totalAttendedHours.add(result.attendedUnits().multiply(hours));
            totalConductedHours = totalConductedHours.add(result.conductedUnits().multiply(hours));
        }

        if (totalConductedHours.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        return totalAttendedHours
            .multiply(HUNDRED)
            .divide(totalConductedHours, DISPLAY_SCALE, ROUNDING);
    }
}
