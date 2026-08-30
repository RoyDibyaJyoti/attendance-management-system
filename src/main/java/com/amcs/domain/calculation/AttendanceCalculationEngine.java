package com.amcs.domain.calculation;

import com.amcs.domain.academic.AcademicPeriod;
import com.amcs.domain.academic.Subject;
import com.amcs.domain.academic.SubjectComponent;
import com.amcs.domain.attendance.AttendanceIntegrityReport;
import com.amcs.domain.attendance.AttendanceIntegrityValidator;
import com.amcs.domain.attendance.AttendanceRecord;
import com.amcs.domain.attendance.Session;
import com.amcs.domain.calculation.result.IntegratedSubjectAttendanceResult;
import com.amcs.domain.calculation.result.OverallAttendanceResult;
import com.amcs.domain.calculation.result.PredictionResult;
import com.amcs.domain.calculation.result.ShortageAnalysis;
import com.amcs.domain.calculation.result.SubjectAttendanceResult;
import com.amcs.domain.enrollment.Enrollment;
import com.amcs.domain.policy.AttendancePolicy;
import com.amcs.domain.policy.IntegratedSubjectPolicy;
import com.amcs.domain.policy.OverallAttendancePolicy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Unified facade for all attendance calculations and integrity inspections.
 */
public class AttendanceCalculationEngine {

    private final SubjectAttendanceCalculator subjectCalculator;
    private final IntegratedSubjectCalculator integratedCalculator;
    private final OverallAttendanceCalculator overallCalculator;
    private final ShortageCalculator shortageCalculator;
    private final PredictionEngine predictionEngine;
    private final AttendanceIntegrityValidator integrityValidator;

    public AttendanceCalculationEngine() {
        this(
            new SubjectAttendanceCalculator(),
            new ShortageCalculator(),
            new AttendanceIntegrityValidator()
        );
    }

    public AttendanceCalculationEngine(
        SubjectAttendanceCalculator subjectCalculator,
        ShortageCalculator shortageCalculator
    ) {
        this(subjectCalculator, shortageCalculator, new AttendanceIntegrityValidator());
    }

    public AttendanceCalculationEngine(
        SubjectAttendanceCalculator subjectCalculator,
        ShortageCalculator shortageCalculator,
        AttendanceIntegrityValidator integrityValidator
    ) {
        this.subjectCalculator = Objects.requireNonNull(subjectCalculator, "subjectCalculator");
        this.shortageCalculator = Objects.requireNonNull(shortageCalculator, "shortageCalculator");
        this.integrityValidator = Objects.requireNonNull(integrityValidator, "integrityValidator");
        this.integratedCalculator = new IntegratedSubjectCalculator(subjectCalculator);
        this.overallCalculator = new OverallAttendanceCalculator();
        this.predictionEngine = new PredictionEngine(shortageCalculator);
    }

    /**
     * Inspects a dataset for data corruption and business rule integrity violations.
     */
    public AttendanceIntegrityReport validateIntegrity(
        UUID studentId,
        Subject subject,
        AcademicPeriod period,
        Enrollment enrollment,
        List<Session> sessions,
        List<AttendanceRecord> records
    ) {
        return integrityValidator.validate(studentId, subject, period, enrollment, sessions, records);
    }

    /**
     * Calculates attendance for a single standard subject (THEORY or LABORATORY).
     */
    public SubjectAttendanceResult calculateSubjectAttendance(
        UUID studentId,
        Subject subject,
        AttendancePolicy policy,
        List<Session> sessions,
        List<AttendanceRecord> records,
        Enrollment enrollment,
        AcademicPeriod period
    ) {
        return subjectCalculator.calculate(
            studentId, subject, policy, sessions, records, enrollment, period);
    }

    /**
     * Calculates attendance for a standard subject under strict data integrity enforcement.
     *
     * @throws com.amcs.domain.attendance.AttendanceIntegrityException if any integrity violation is found
     */
    public SubjectAttendanceResult calculateSubjectAttendanceStrict(
        UUID studentId,
        Subject subject,
        AttendancePolicy policy,
        List<Session> sessions,
        List<AttendanceRecord> records,
        Enrollment enrollment,
        AcademicPeriod period
    ) {
        return subjectCalculator.calculateWithStrictValidation(
            studentId, subject, policy, sessions, records, enrollment, period);
    }

    /**
     * Calculates attendance for a THEORY_INTEGRATED_LABORATORY subject.
     */
    public IntegratedSubjectAttendanceResult calculateIntegratedSubjectAttendance(
        UUID studentId,
        Subject subject,
        SubjectComponent theoryComponent,
        SubjectComponent labComponent,
        IntegratedSubjectPolicy integratedPolicy,
        List<Session> allSessions,
        List<AttendanceRecord> records,
        Enrollment enrollment,
        AcademicPeriod period
    ) {
        return integratedCalculator.calculate(
            studentId, subject, theoryComponent, labComponent,
            integratedPolicy, allSessions, records, enrollment, period);
    }

    /**
     * Aggregates subject-level results into an overall attendance result.
     */
    public OverallAttendanceResult calculateOverallAttendance(
        UUID studentId,
        List<SubjectAttendanceResult> subjectResults,
        Map<UUID, Integer> creditHoursById,
        OverallAttendancePolicy policy,
        AcademicPeriod period
    ) {
        return overallCalculator.calculate(
            studentId, subjectResults, creditHoursById, policy, period);
    }

    /**
     * Aggregates subject-level results into an overall attendance result with contact hours per unit.
     */
    public OverallAttendanceResult calculateOverallAttendance(
        UUID studentId,
        List<SubjectAttendanceResult> subjectResults,
        Map<UUID, Integer> creditHoursById,
        Map<UUID, BigDecimal> hoursPerUnitBySubject,
        OverallAttendancePolicy policy,
        AcademicPeriod period
    ) {
        return overallCalculator.calculate(
            studentId, subjectResults, creditHoursById, hoursPerUnitBySubject, policy, period);
    }

    /**
     * Produces a standalone ShortageAnalysis from an existing SubjectAttendanceResult.
     */
    public ShortageAnalysis analyzeShortage(SubjectAttendanceResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new ShortageAnalysis(
            result.studentId(),
            result.subjectId(),
            result.conductedUnits(),
            result.attendedUnits(),
            result.attendancePercentage(),
            result.minimumThresholdPercentage(),
            result.isShortage(),
            result.shortageUnits(),
            result.surplusUnits(),
            Instant.now()
        );
    }

    /**
     * Predicts future attendance outcomes based on remaining scheduled/expected units.
     */
    public PredictionResult predictFutureAttendance(
        UUID studentId,
        UUID subjectId,
        SubjectAttendanceResult currentResult,
        int remainingUnits
    ) {
        return predictionEngine.predict(studentId, subjectId, currentResult, remainingUnits);
    }

    public SubjectAttendanceCalculator getSubjectCalculator() {
        return subjectCalculator;
    }

    public IntegratedSubjectCalculator getIntegratedCalculator() {
        return integratedCalculator;
    }

    public OverallAttendanceCalculator getOverallCalculator() {
        return overallCalculator;
    }

    public ShortageCalculator getShortageCalculator() {
        return shortageCalculator;
    }

    public PredictionEngine getPredictionEngine() {
        return predictionEngine;
    }

    public AttendanceIntegrityValidator getIntegrityValidator() {
        return integrityValidator;
    }
}
