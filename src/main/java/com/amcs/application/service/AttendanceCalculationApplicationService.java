package com.amcs.application.service;

import com.amcs.application.dto.calculation.OverallAttendanceSummaryResponse;
import com.amcs.application.dto.calculation.ShortageProjectionResponse;
import com.amcs.application.dto.calculation.StudentAttendanceOverviewResponse;
import com.amcs.application.dto.calculation.SubjectAttendanceSummaryResponse;
import com.amcs.application.exception.ResourceNotFoundException;
import com.amcs.application.port.out.AcademicPeriodRepositoryPort;
import com.amcs.application.port.out.AttendancePolicyRepositoryPort;
import com.amcs.application.port.out.AttendanceRecordRepositoryPort;
import com.amcs.application.port.out.EnrollmentRepositoryPort;
import com.amcs.application.port.out.OverallAttendancePolicyRepositoryPort;
import com.amcs.application.port.out.SessionRepositoryPort;
import com.amcs.application.port.out.StudentRepositoryPort;
import com.amcs.application.port.out.SubjectRepositoryPort;
import com.amcs.domain.academic.AcademicPeriod;
import com.amcs.domain.academic.Subject;
import com.amcs.domain.attendance.AttendanceRecord;
import com.amcs.domain.attendance.Session;
import com.amcs.domain.calculation.AttendanceCalculationEngine;
import com.amcs.domain.calculation.ShortageCalculator;
import com.amcs.domain.calculation.result.OverallAttendanceResult;
import com.amcs.domain.calculation.result.SubjectAttendanceResult;
import com.amcs.domain.enrollment.Enrollment;
import com.amcs.domain.policy.AttendancePolicy;
import com.amcs.domain.policy.OverallAggregationStrategy;
import com.amcs.domain.policy.OverallAttendancePolicy;
import com.amcs.infrastructure.persistence.entity.OverallAttendancePolicyEntity;
import com.amcs.infrastructure.persistence.entity.StudentEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service orchestrating facts for the pure domain calculation engine.
 * <p><strong>Pure Engine Isolation:</strong> Contains ZERO attendance formulas or policy math.
 * All calculations are executed by {@link AttendanceCalculationEngine} and {@link ShortageCalculator}.
 */
@Service
@Transactional(readOnly = true)
public class AttendanceCalculationApplicationService {

    private final StudentRepositoryPort studentPort;
    private final SubjectRepositoryPort subjectPort;
    private final SessionRepositoryPort sessionPort;
    private final AttendanceRecordRepositoryPort recordPort;
    private final EnrollmentRepositoryPort enrollmentPort;
    private final AttendancePolicyRepositoryPort policyPort;
    private final OverallAttendancePolicyRepositoryPort overallPolicyPort;
    private final AcademicPeriodRepositoryPort periodPort;
    private final AttendanceCalculationEngine calculationEngine;
    private final com.amcs.application.security.ApplicationAuthorizationService authorizationService;

    public AttendanceCalculationApplicationService(
        StudentRepositoryPort studentPort,
        SubjectRepositoryPort subjectPort,
        SessionRepositoryPort sessionPort,
        AttendanceRecordRepositoryPort recordPort,
        EnrollmentRepositoryPort enrollmentPort,
        AttendancePolicyRepositoryPort policyPort,
        OverallAttendancePolicyRepositoryPort overallPolicyPort,
        AcademicPeriodRepositoryPort periodPort,
        com.amcs.application.security.ApplicationAuthorizationService authorizationService
    ) {
        this.studentPort = Objects.requireNonNull(studentPort, "studentPort");
        this.subjectPort = Objects.requireNonNull(subjectPort, "subjectPort");
        this.sessionPort = Objects.requireNonNull(sessionPort, "sessionPort");
        this.recordPort = Objects.requireNonNull(recordPort, "recordPort");
        this.enrollmentPort = Objects.requireNonNull(enrollmentPort, "enrollmentPort");
        this.policyPort = Objects.requireNonNull(policyPort, "policyPort");
        this.overallPolicyPort = Objects.requireNonNull(overallPolicyPort, "overallPolicyPort");
        this.periodPort = Objects.requireNonNull(periodPort, "periodPort");
        this.calculationEngine = new AttendanceCalculationEngine();
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService");
    }

    public SubjectAttendanceSummaryResponse calculateSubjectAttendance(
        UUID studentId, UUID subjectId, UUID sectionId, UUID policyId, UUID academicPeriodId
    ) {
        if (authorizationService != null) {
            authorizationService.requireSubjectAttendanceReadAccess(studentId, subjectId, sectionId, academicPeriodId);
        }
        StudentEntity student = studentPort.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        Subject subject = subjectPort.findById(subjectId)
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + subjectId));
        AttendancePolicy policy = policyPort.findById(policyId)
            .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + policyId));
        AcademicPeriod period = periodPort.findById(academicPeriodId)
            .orElseThrow(() -> new ResourceNotFoundException("Academic period not found: " + academicPeriodId));
        Enrollment enrollment = enrollmentPort.findActiveEnrollment(studentId, sectionId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Active enrollment not found for student [%s] in section [%s]".formatted(studentId, sectionId)));

        List<Session> sessions = sessionPort.findBySubjectAndPeriod(subjectId, academicPeriodId);
        List<UUID> sessionIds = sessions.stream().map(Session::id).toList();
        List<AttendanceRecord> records = recordPort.findByStudentAndSessions(studentId, sessionIds);

        // Execute pure domain calculation engine
        SubjectAttendanceResult result = calculationEngine.calculateSubjectAttendance(
            studentId, subject, policy, sessions, records, enrollment, period);

        return toSubjectSummaryResponse(result, subject, policy);
    }

    public StudentAttendanceOverviewResponse getStudentAttendanceOverview(
        UUID studentId, UUID sectionId, UUID policyId, UUID academicPeriodId
    ) {
        if (authorizationService != null) {
            authorizationService.requireStudentSelfAccess(studentId);
        }
        StudentEntity student = studentPort.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        AcademicPeriod period = periodPort.findById(academicPeriodId)
            .orElseThrow(() -> new ResourceNotFoundException("Academic period not found: " + academicPeriodId));

        List<Subject> allSubjects = subjectPort.findAll();
        List<SubjectAttendanceSummaryResponse> subjectSummaries = new ArrayList<>();

        for (Subject subject : allSubjects) {
            try {
                SubjectAttendanceSummaryResponse summary = calculateSubjectAttendance(
                    studentId, subject.id(), sectionId, policyId, academicPeriodId);
                subjectSummaries.add(summary);
            } catch (Exception ignored) {
                // Subject not enrolled or not offered in this section
            }
        }

        return new StudentAttendanceOverviewResponse(
            student.getId(),
            student.getName(),
            student.getRegistrationNumber(),
            academicPeriodId,
            subjectSummaries
        );
    }

    public OverallAttendanceSummaryResponse calculateOverallAttendance(
        UUID studentId, UUID sectionId, UUID policyId, UUID academicPeriodId, String strategyOverride
    ) {
        if (authorizationService != null) {
            authorizationService.requireStudentSelfAccess(studentId);
        }
        StudentEntity student = studentPort.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        AcademicPeriod period = periodPort.findById(academicPeriodId)
            .orElseThrow(() -> new ResourceNotFoundException("Academic period not found: " + academicPeriodId));

        OverallAggregationStrategy strategy = (strategyOverride != null)
            ? OverallAggregationStrategy.valueOf(strategyOverride)
            : OverallAggregationStrategy.ARITHMETIC_MEAN;

        OverallAttendancePolicy overallPolicy = new OverallAttendancePolicy(
            UUID.randomUUID(),
            "Overall Policy",
            1,
            strategy,
            new BigDecimal("75.00"),
            period.startDate().atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
            Optional.empty()
        );

        List<Subject> allSubjects = subjectPort.findAll();
        List<SubjectAttendanceResult> results = new ArrayList<>();

        for (Subject subject : allSubjects) {
            try {
                AttendancePolicy policy = policyPort.findById(policyId).orElse(null);
                Enrollment enrollment = enrollmentPort.findActiveEnrollment(studentId, sectionId).orElse(null);
                if (policy != null && enrollment != null) {
                    List<Session> sessions = sessionPort.findBySubjectAndPeriod(subject.id(), academicPeriodId);
                    List<UUID> sessionIds = sessions.stream().map(Session::id).toList();
                    List<AttendanceRecord> records = recordPort.findByStudentAndSessions(studentId, sessionIds);

                    SubjectAttendanceResult res = calculationEngine.calculateSubjectAttendance(
                        studentId, subject, policy, sessions, records, enrollment, period);
                    results.add(res);
                }
            } catch (Exception ignored) {}
        }

        java.util.Map<UUID, Integer> credits = allSubjects.stream()
            .collect(java.util.stream.Collectors.toMap(Subject::id, Subject::creditHours, (a, b) -> a));

        OverallAttendanceResult overallResult = calculationEngine.calculateOverallAttendance(
            studentId, results, credits, overallPolicy, period);

        return new OverallAttendanceSummaryResponse(
            student.getId(),
            overallPolicy.name(),
            overallPolicy.aggregationStrategy().name(),
            overallPolicy.minimumThresholdPercentage(),
            overallResult.overallPercentage(),
            overallResult.isShortage() ? "SHORTAGE" : "ADEQUATE",
            !overallResult.isShortage(),
            overallResult.isShortage(),
            overallResult.subjectResults().size()
        );
    }

    public ShortageProjectionResponse calculateShortageProjection(
        UUID studentId, UUID subjectId, UUID sectionId, UUID policyId, UUID academicPeriodId, int projectedRemainingUnits
    ) {
        SubjectAttendanceSummaryResponse current = calculateSubjectAttendance(
            studentId, subjectId, sectionId, policyId, academicPeriodId);

        // Invoke pure ShortageCalculator via calculationEngine
        int xMin = calculationEngine.getShortageCalculator().computeMinimumUnitsRequired(
            current.attendedUnits(),
            current.conductedUnits(),
            current.thresholdPercentage()
        );

        int yMax = calculationEngine.getShortageCalculator().computeMaximumUnitsMissable(
            current.attendedUnits(),
            current.conductedUnits(),
            projectedRemainingUnits,
            current.thresholdPercentage()
        );

        boolean isRecoverable = (xMin <= projectedRemainingUnits) && (xMin >= 0);

        return new ShortageProjectionResponse(
            studentId,
            subjectId,
            current.attendancePercentage(),
            current.thresholdPercentage(),
            current.shortageUnits(),
            current.surplusUnits(),
            xMin,
            yMax,
            projectedRemainingUnits,
            isRecoverable
        );
    }

    private SubjectAttendanceSummaryResponse toSubjectSummaryResponse(
        SubjectAttendanceResult result, Subject subject, AttendancePolicy policy
    ) {
        return new SubjectAttendanceSummaryResponse(
            result.studentId(),
            subject.id(),
            subject.code(),
            subject.name(),
            policy.name(),
            policy.version(),
            policy.minimumThresholdPercentage(),
            result.conductedUnits(),
            result.attendedUnits(),
            result.attendancePercentage(),
            result.classification().name(),
            result.isAdequate(),
            result.isShortage(),
            result.shortageUnits(),
            result.surplusUnits(),
            result.missingRecordCount(),
            result.isIncomplete()
        );
    }
}
