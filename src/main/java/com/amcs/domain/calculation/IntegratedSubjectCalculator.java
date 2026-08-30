package com.amcs.domain.calculation;

import com.amcs.domain.academic.ComponentType;
import com.amcs.domain.academic.Subject;
import com.amcs.domain.academic.AcademicPeriod;
import com.amcs.domain.academic.SubjectComponent;
import com.amcs.domain.attendance.AttendanceRecord;
import com.amcs.domain.attendance.Session;
import com.amcs.domain.attendance.SessionType;
import com.amcs.domain.calculation.result.ComponentAttendanceResult;
import com.amcs.domain.calculation.result.IntegratedSubjectAttendanceResult;
import com.amcs.domain.calculation.result.SubjectAttendanceResult;
import com.amcs.domain.enrollment.Enrollment;
import com.amcs.domain.policy.IntegratedCompositeStrategy;
import com.amcs.domain.policy.IntegratedSubjectPolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Calculates attendance for a {@link com.amcs.domain.academic.CourseType#THEORY_INTEGRATED_LABORATORY}
 * subject by computing each component (theory and lab) independently and then
 * combining them according to the configured {@link IntegratedSubjectPolicy}.
 *
 * <h3>Separation principle</h3>
 * <p>Theory sessions are filtered by {@link SessionType#THEORY}; lab sessions
 * by {@link SessionType#LAB}. Each set is passed to {@link SubjectAttendanceCalculator}
 * with its own per-component {@link com.amcs.domain.policy.AttendancePolicy}.
 * This is the correct way to apply different rules to each component.
 *
 * <h3>COMBINED_ALL_SESSIONS strategy note</h3>
 * <p>When {@link IntegratedCompositeStrategy#COMBINED_ALL_SESSIONS} is used, ALL
 * sessions are passed together without type filtering, so the combined policy's
 * contributions apply uniformly to both THEORY and LAB sessions.
 */
public class IntegratedSubjectCalculator {

    private final SubjectAttendanceCalculator subjectCalculator;

    public IntegratedSubjectCalculator(SubjectAttendanceCalculator subjectCalculator) {
        this.subjectCalculator = Objects.requireNonNull(subjectCalculator, "subjectCalculator");
    }

    /**
     * Computes the integrated attendance result.
     *
     * @param student                the subject entity
     * @param theoryComponent        policy+component definition for theory
     * @param labComponent           policy+component definition for lab
     * @param integratedPolicy       how the two components are combined
     * @param allSessions            all sessions for this subject (theory and lab)
     * @param attendanceRecords      attendance records for this student
     * @param enrollment             student enrollment record for the section
     * @param period                 academic period
     * @return the combined result
     */
    public IntegratedSubjectAttendanceResult calculate(
        UUID studentId,
        Subject subject,
        SubjectComponent theoryComponent,
        SubjectComponent labComponent,
        IntegratedSubjectPolicy integratedPolicy,
        List<Session> allSessions,
        List<AttendanceRecord> attendanceRecords,
        Enrollment enrollment,
        AcademicPeriod period
    ) {
        Objects.requireNonNull(studentId, "studentId must not be null");
        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(theoryComponent, "theoryComponent must not be null");
        Objects.requireNonNull(labComponent, "labComponent must not be null");
        Objects.requireNonNull(integratedPolicy, "integratedPolicy must not be null");
        Objects.requireNonNull(allSessions, "allSessions must not be null");
        Objects.requireNonNull(attendanceRecords, "attendanceRecords must not be null");
        Objects.requireNonNull(enrollment, "enrollment must not be null");
        Objects.requireNonNull(period, "period must not be null");

        // ── Split sessions by type ───────────────────────────────────────────
        List<Session> theorySessions = allSessions.stream()
            .filter(s -> s.sessionType() == SessionType.THEORY)
            .collect(Collectors.toUnmodifiableList());
        List<Session> labSessions = allSessions.stream()
            .filter(s -> s.sessionType() == SessionType.LAB)
            .collect(Collectors.toUnmodifiableList());

        // ── Calculate each component independently ───────────────────────────
        SubjectAttendanceResult theoryResult = subjectCalculator.calculate(
            studentId, subject, theoryComponent.policy(),
            theorySessions, attendanceRecords, enrollment, period);

        SubjectAttendanceResult labResult = subjectCalculator.calculate(
            studentId, subject, labComponent.policy(),
            labSessions, attendanceRecords, enrollment, period);

        ComponentAttendanceResult theory = new ComponentAttendanceResult(ComponentType.THEORY, theoryResult);
        ComponentAttendanceResult lab = new ComponentAttendanceResult(ComponentType.LAB, labResult);

        return switch (integratedPolicy.strategy()) {
            case SEPARATE_THRESHOLDS -> buildSeparateThresholdsResult(
                studentId, subject, period, theory, lab, integratedPolicy);

            case COMBINED_ALL_SESSIONS -> buildCombinedResult(
                studentId, subject, period, theory, lab, integratedPolicy,
                allSessions, attendanceRecords, enrollment);

            case WEIGHTED_AVERAGE -> buildWeightedAverageResult(
                studentId, subject, period, theory, lab, integratedPolicy);
        };
    }

    // =========================================================================
    // Strategy implementations
    // =========================================================================

    private IntegratedSubjectAttendanceResult buildSeparateThresholdsResult(
        UUID studentId, Subject subject, AcademicPeriod period,
        ComponentAttendanceResult theory, ComponentAttendanceResult lab,
        IntegratedSubjectPolicy policy
    ) {
        // Shortage if EITHER component is in shortage (or UNDEFINED is treated as no shortage)
        boolean shortage = theory.result().isShortage() || lab.result().isShortage();
        return new IntegratedSubjectAttendanceResult(
            studentId, subject.id(), period,
            IntegratedCompositeStrategy.SEPARATE_THRESHOLDS,
            theory, lab,
            Optional.empty(), Optional.empty(), Optional.empty(),
            shortage, Instant.now()
        );
    }

    private IntegratedSubjectAttendanceResult buildCombinedResult(
        UUID studentId, Subject subject, AcademicPeriod period,
        ComponentAttendanceResult theory, ComponentAttendanceResult lab,
        IntegratedSubjectPolicy policy,
        List<Session> allSessions,
        List<AttendanceRecord> attendanceRecords,
        Enrollment enrollment
    ) {
        // Recalculate with ALL sessions under the combined policy
        SubjectAttendanceResult combined = subjectCalculator.calculate(
            studentId, subject, policy.combinedPolicy().orElseThrow(),
            allSessions, attendanceRecords, enrollment,
            theory.result().period()); // period is the same

        return new IntegratedSubjectAttendanceResult(
            studentId, subject.id(), period,
            IntegratedCompositeStrategy.COMBINED_ALL_SESSIONS,
            theory, lab,
            Optional.of(combined), Optional.empty(), Optional.empty(),
            combined.isShortage(), Instant.now()
        );
    }

    private IntegratedSubjectAttendanceResult buildWeightedAverageResult(
        UUID studentId, Subject subject, AcademicPeriod period,
        ComponentAttendanceResult theory, ComponentAttendanceResult lab,
        IntegratedSubjectPolicy policy
    ) {
        BigDecimal tw = policy.theoryWeight().orElseThrow();
        BigDecimal lw = policy.labWeight().orElseThrow();
        BigDecimal compositeThreshold = policy.compositeThresholdPercentage().orElseThrow();

        // Use 0% as the component percentage if UNDEFINED (no sessions conducted)
        BigDecimal theoryPct = theory.result().isUndefined()
            ? BigDecimal.ZERO : theory.result().attendancePercentage();
        BigDecimal labPct = lab.result().isUndefined()
            ? BigDecimal.ZERO : lab.result().attendancePercentage();

        // composite = tw × theory% + lw × lab%
        BigDecimal composite = tw.multiply(theoryPct)
            .add(lw.multiply(labPct))
            .setScale(SubjectAttendanceResult.PERCENTAGE_SCALE, RoundingMode.HALF_UP);

        boolean shortage = composite.compareTo(compositeThreshold) < 0;

        return new IntegratedSubjectAttendanceResult(
            studentId, subject.id(), period,
            IntegratedCompositeStrategy.WEIGHTED_AVERAGE,
            theory, lab,
            Optional.empty(),
            Optional.of(composite),
            Optional.of(compositeThreshold),
            shortage, Instant.now()
        );
    }
}
