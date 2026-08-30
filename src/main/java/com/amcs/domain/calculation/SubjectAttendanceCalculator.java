package com.amcs.domain.calculation;

import com.amcs.domain.academic.AcademicPeriod;
import com.amcs.domain.academic.Subject;
import com.amcs.domain.attendance.AttendanceClassification;
import com.amcs.domain.attendance.AttendanceRecord;
import com.amcs.domain.attendance.AttendanceStatus;
import com.amcs.domain.attendance.Session;
import com.amcs.domain.calculation.result.SubjectAttendanceResult;
import com.amcs.domain.enrollment.Enrollment;
import com.amcs.domain.policy.AttendancePolicy;
import com.amcs.domain.policy.MissingRecordStrategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Computes subject-level attendance for a single student.
 *
 * <h3>Algorithm</h3>
 * <ol>
 *   <li>Filter sessions to only CONDUCTED ones within the academic period.</li>
 *   <li>Filter sessions within the student's enrollment window (DI-003).</li>
 *   <li>Filter sessions by lab group if the session is group-specific (Rule L-004).</li>
 *   <li>For each eligible session, look up the student's AttendanceRecord.</li>
 *   <li>If no record exists for a CONDUCTED session:
 *       <ul>
 *         <li>Apply configured {@link MissingRecordStrategy} (TREAT_AS_ABSENT,
 *             EXCLUDE_FROM_CALCULATION, MARK_AS_INCOMPLETE).</li>
 *         <li>Track missing record count for auditability.</li>
 *       </ul>
 *   </li>
 *   <li>Compute denominator = Σ(session.conductedUnits) over eligible sessions.</li>
 *   <li>Compute numerator = Σ(session.conductedUnits × policy.getContribution(record.status)).</li>
 *   <li>Condonation step: SKIPPED — condonation is strictly a post-calculation layer.</li>
 *   <li>Compute percentage = (numerator / denominator) × 100, rounded HALF_UP to 2 decimal places.</li>
 *   <li>Classify as ADEQUATE, SHORTAGE, INCOMPLETE, or UNDEFINED (if denominator = 0).</li>
 *   <li>Compute shortageUnits and surplusUnits relative to the threshold.</li>
 * </ol>
 */
public class SubjectAttendanceCalculator {

    /** Scale for intermediate BigDecimal arithmetic (higher precision to avoid rounding accumulation). */
    static final int CALC_SCALE = 10;

    /** Final display scale for percentages. */
    static final int PERCENTAGE_SCALE = SubjectAttendanceResult.PERCENTAGE_SCALE;

    static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    static final BigDecimal HUNDRED = new BigDecimal("100");

    /**
     * Calculates subject-level attendance.
     */
    public SubjectAttendanceResult calculate(
        UUID studentId,
        Subject subject,
        AttendancePolicy policy,
        List<Session> sessions,
        List<AttendanceRecord> attendanceRecords,
        Enrollment enrollment,
        AcademicPeriod period
    ) {
        Objects.requireNonNull(studentId, "studentId must not be null");
        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(policy, "policy must not be null");
        Objects.requireNonNull(sessions, "sessions must not be null");
        Objects.requireNonNull(attendanceRecords, "attendanceRecords must not be null");
        Objects.requireNonNull(enrollment, "enrollment must not be null");
        Objects.requireNonNull(period, "period must not be null");

        // Build a lookup: sessionId → AttendanceRecord (for this student)
        // Keep first on collision (duplicates are flagged via AttendanceIntegrityValidator)
        Map<UUID, AttendanceRecord> recordBySession = attendanceRecords.stream()
            .filter(r -> studentId.equals(r.studentId()))
            .collect(Collectors.toMap(
                AttendanceRecord::sessionId,
                r -> r,
                (first, second) -> first
            ));

        BigDecimal totalConducted = BigDecimal.ZERO;
        BigDecimal totalAttended = BigDecimal.ZERO;
        int missingRecordCount = 0;

        for (Session session : sessions) {
            // ── Gate 1: Only CONDUCTED sessions contribute ───────────────────
            if (!session.isCountable()) continue;

            // ── Gate 2: Session must fall within the academic period ─────────
            if (!period.contains(session.sessionDate())) continue;

            // ── Gate 3: Student must be enrolled on the session date ─────────
            if (!enrollment.isActiveOn(session.sessionDate())) continue;

            // ── Gate 4: Lab group check ──────────────────────────────────────
            if (!enrollment.isEligibleForLabGroup(session.labGroupId())) continue;

            BigDecimal units = BigDecimal.valueOf(session.conductedUnits());
            AttendanceRecord record = recordBySession.get(session.id());

            if (record == null) {
                missingRecordCount++;
                switch (policy.missingRecordStrategy()) {
                    case EXCLUDE_FROM_CALCULATION -> {
                        // Session completely omitted from denominator and numerator
                        continue;
                    }
                    case TREAT_AS_ABSENT, MARK_AS_INCOMPLETE -> {
                        totalConducted = totalConducted.add(units);
                        BigDecimal contribution = policy.getContribution(AttendanceStatus.ABSENT);
                        BigDecimal attendedForSession = units
                            .multiply(contribution)
                            .setScale(CALC_SCALE, ROUNDING);
                        totalAttended = totalAttended.add(attendedForSession);
                    }
                }
            } else {
                totalConducted = totalConducted.add(units);
                BigDecimal contribution = policy.getContribution(record.status());
                BigDecimal attendedForSession = units
                    .multiply(contribution)
                    .setScale(CALC_SCALE, ROUNDING);
                totalAttended = totalAttended.add(attendedForSession);
            }
        }

        return buildResult(studentId, subject, policy, period, totalConducted, totalAttended, missingRecordCount);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private SubjectAttendanceResult buildResult(
        UUID studentId,
        Subject subject,
        AttendancePolicy policy,
        AcademicPeriod period,
        BigDecimal conducted,
        BigDecimal attended,
        int missingRecordCount
    ) {
        // ── Handle zero-denominator (no conducted sessions in this period) ───
        if (conducted.compareTo(BigDecimal.ZERO) == 0) {
            AttendanceClassification classification = (missingRecordCount > 0
                && policy.missingRecordStrategy() == MissingRecordStrategy.MARK_AS_INCOMPLETE)
                ? AttendanceClassification.INCOMPLETE
                : AttendanceClassification.UNDEFINED;

            return new SubjectAttendanceResult(
                studentId, subject.id(), policy.id(), policy.version(), period,
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, policy.minimumThresholdPercentage(),
                classification,
                BigDecimal.ZERO, BigDecimal.ZERO,
                policy.missingRecordStrategy(), missingRecordCount,
                Instant.now()
            );
        }

        // Safety clamp: attendedUnits ∈ [0, conducted]
        if (attended.compareTo(BigDecimal.ZERO) < 0) attended = BigDecimal.ZERO;
        if (attended.compareTo(conducted) > 0) attended = conducted;

        // Compute percentage: (attended / conducted) × 100
        BigDecimal percentage = attended
            .multiply(HUNDRED)
            .divide(conducted, PERCENTAGE_SCALE, ROUNDING);

        BigDecimal threshold = policy.minimumThresholdPercentage();

        // Classify
        AttendanceClassification classification;
        if (missingRecordCount > 0 && policy.missingRecordStrategy() == MissingRecordStrategy.MARK_AS_INCOMPLETE) {
            classification = AttendanceClassification.INCOMPLETE;
        } else if (percentage.compareTo(threshold) >= 0) {
            classification = AttendanceClassification.ADEQUATE;
        } else {
            classification = AttendanceClassification.SHORTAGE;
        }

        // Compute shortage / surplus units
        BigDecimal requiredAttended = threshold
            .multiply(conducted)
            .divide(HUNDRED, CALC_SCALE, ROUNDING);

        BigDecimal shortage = BigDecimal.ZERO;
        BigDecimal surplus = BigDecimal.ZERO;

        if (classification == AttendanceClassification.SHORTAGE
            || (classification == AttendanceClassification.INCOMPLETE && percentage.compareTo(threshold) < 0)) {
            shortage = requiredAttended.subtract(attended)
                .setScale(PERCENTAGE_SCALE, ROUNDING)
                .max(BigDecimal.ZERO);
        } else {
            surplus = attended.subtract(requiredAttended)
                .setScale(PERCENTAGE_SCALE, ROUNDING)
                .max(BigDecimal.ZERO);
        }

        return new SubjectAttendanceResult(
            studentId, subject.id(), policy.id(), policy.version(), period,
            conducted.setScale(PERCENTAGE_SCALE, ROUNDING),
            attended.setScale(PERCENTAGE_SCALE, ROUNDING),
            percentage,
            threshold,
            classification,
            shortage,
            surplus,
            policy.missingRecordStrategy(),
            missingRecordCount,
            Instant.now()
        );
    }

    /**
     * Calculates subject attendance under strict data integrity enforcement.
     *
     * @throws com.amcs.domain.attendance.AttendanceIntegrityException if any integrity violations are detected
     */
    public SubjectAttendanceResult calculateWithStrictValidation(
        UUID studentId,
        Subject subject,
        AttendancePolicy policy,
        List<Session> sessions,
        List<AttendanceRecord> attendanceRecords,
        Enrollment enrollment,
        AcademicPeriod period
    ) {
        var report = new com.amcs.domain.attendance.AttendanceIntegrityValidator()
            .validate(studentId, subject, period, enrollment, sessions, attendanceRecords);
        if (report.hasViolations()) {
            throw new com.amcs.domain.attendance.AttendanceIntegrityException(report);
        }
        return calculate(studentId, subject, policy, sessions, attendanceRecords, enrollment, period);
    }
}
