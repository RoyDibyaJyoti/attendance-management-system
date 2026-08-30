package com.amcs.application.service;

import com.amcs.application.port.out.AttendancePolicyRepositoryPort;
import com.amcs.application.port.out.AttendanceRecordRepositoryPort;
import com.amcs.application.port.out.EnrollmentRepositoryPort;
import com.amcs.application.port.out.SessionRepositoryPort;
import com.amcs.application.port.out.SubjectRepositoryPort;
import com.amcs.domain.academic.AcademicPeriod;
import com.amcs.domain.academic.Subject;
import com.amcs.domain.attendance.AttendanceRecord;
import com.amcs.domain.attendance.Session;
import com.amcs.domain.calculation.AttendanceCalculationEngine;
import com.amcs.domain.calculation.result.SubjectAttendanceResult;
import com.amcs.domain.enrollment.Enrollment;
import com.amcs.domain.policy.AttendancePolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Application service orchestrating the loading of persisted attendance facts,
 * invoking the pure domain calculation engine, and returning immutable domain results.
 *
 * <p><strong>Pure Engine Isolation:</strong> {@link AttendanceCalculationEngine} receives
 * only pure domain models and remains completely oblivious to Spring, JPA, or PostgreSQL.
 */
@Service
@Transactional(readOnly = true)
public class AttendanceCalculationOrchestrator {

    private final SubjectRepositoryPort subjectPort;
    private final SessionRepositoryPort sessionPort;
    private final AttendanceRecordRepositoryPort recordPort;
    private final EnrollmentRepositoryPort enrollmentPort;
    private final AttendancePolicyRepositoryPort policyPort;
    private final AttendanceCalculationEngine engine;

    public AttendanceCalculationOrchestrator(
        SubjectRepositoryPort subjectPort,
        SessionRepositoryPort sessionPort,
        AttendanceRecordRepositoryPort recordPort,
        EnrollmentRepositoryPort enrollmentPort,
        AttendancePolicyRepositoryPort policyPort
    ) {
        this.subjectPort = Objects.requireNonNull(subjectPort, "subjectPort");
        this.sessionPort = Objects.requireNonNull(sessionPort, "sessionPort");
        this.recordPort = Objects.requireNonNull(recordPort, "recordPort");
        this.enrollmentPort = Objects.requireNonNull(enrollmentPort, "enrollmentPort");
        this.policyPort = Objects.requireNonNull(policyPort, "policyPort");
        this.engine = new AttendanceCalculationEngine();
    }

    /**
     * Loads persisted facts for a student in a subject, maps them to domain records,
     * and runs the pure domain calculation engine.
     */
    public SubjectAttendanceResult calculateSubjectAttendance(
        UUID studentId,
        UUID subjectId,
        UUID sectionId,
        UUID policyId,
        UUID academicPeriodId,
        AcademicPeriod period
    ) {
        Subject subject = subjectPort.findById(subjectId)
            .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectId));

        AttendancePolicy policy = policyPort.findById(policyId)
            .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyId));

        Enrollment enrollment = enrollmentPort.findActiveEnrollment(studentId, sectionId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Active enrollment not found for student [%s] in section [%s]".formatted(studentId, sectionId)));

        List<Session> sessions = sessionPort.findBySubjectAndPeriod(subjectId, academicPeriodId);

        List<UUID> sessionIds = sessions.stream().map(Session::id).toList();
        List<AttendanceRecord> records = recordPort.findByStudentAndSessions(studentId, sessionIds);

        // Execute pure domain calculation
        return engine.calculateSubjectAttendance(
            studentId, subject, policy, sessions, records, enrollment, period);
    }
}
