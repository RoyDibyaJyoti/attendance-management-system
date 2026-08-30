package com.amcs.application.service;

import com.amcs.application.dto.attendance.AttendanceCorrectionResponse;
import com.amcs.application.dto.attendance.AttendanceRecordItemDto;
import com.amcs.application.dto.attendance.AttendanceRecordResponse;
import com.amcs.application.dto.attendance.CorrectAttendanceRecordRequest;
import com.amcs.application.dto.attendance.RecordAttendanceBatchRequest;
import com.amcs.application.dto.attendance.SessionAttendanceSummaryResponse;
import com.amcs.application.exception.InvalidBusinessOperationException;
import com.amcs.application.exception.InvalidSessionTransitionException;
import com.amcs.application.exception.ResourceNotFoundException;
import com.amcs.application.exception.SessionStateConflictException;
import com.amcs.application.exception.StudentNotEligibleException;
import com.amcs.application.port.out.AttendanceRecordRepositoryPort;
import com.amcs.application.port.out.EnrollmentRepositoryPort;
import com.amcs.application.port.out.LabGroupRepositoryPort;
import com.amcs.application.port.out.SessionRepositoryPort;
import com.amcs.domain.attendance.AttendanceIntegrityReport;
import com.amcs.domain.attendance.AttendanceIntegrityValidator;
import com.amcs.domain.attendance.AttendanceRecord;
import com.amcs.domain.attendance.AttendanceStatus;
import com.amcs.domain.attendance.Session;
import com.amcs.domain.attendance.SessionStatus;
import com.amcs.domain.enrollment.Enrollment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AttendanceRecordingApplicationService {

    private final SessionRepositoryPort sessionPort;
    private final AttendanceRecordRepositoryPort recordPort;
    private final EnrollmentRepositoryPort enrollmentPort;
    private final LabGroupRepositoryPort labGroupPort;
    private final com.amcs.application.port.out.SubjectRepositoryPort subjectPort;
    private final com.amcs.application.port.out.AcademicPeriodRepositoryPort periodPort;
    private final AttendanceIntegrityValidator integrityValidator;
    private final com.amcs.application.security.ApplicationAuthorizationService authorizationService;

    public AttendanceRecordingApplicationService(
        SessionRepositoryPort sessionPort,
        AttendanceRecordRepositoryPort recordPort,
        EnrollmentRepositoryPort enrollmentPort,
        LabGroupRepositoryPort labGroupPort,
        com.amcs.application.port.out.SubjectRepositoryPort subjectPort,
        com.amcs.application.port.out.AcademicPeriodRepositoryPort periodPort
    ) {
        this(sessionPort, recordPort, enrollmentPort, labGroupPort, subjectPort, periodPort, null);
    }

    public AttendanceRecordingApplicationService(
        SessionRepositoryPort sessionPort,
        AttendanceRecordRepositoryPort recordPort,
        EnrollmentRepositoryPort enrollmentPort,
        LabGroupRepositoryPort labGroupPort,
        com.amcs.application.port.out.SubjectRepositoryPort subjectPort,
        com.amcs.application.port.out.AcademicPeriodRepositoryPort periodPort,
        com.amcs.application.security.ApplicationAuthorizationService authorizationService
    ) {
        this.sessionPort = Objects.requireNonNull(sessionPort, "sessionPort");
        this.recordPort = Objects.requireNonNull(recordPort, "recordPort");
        this.enrollmentPort = Objects.requireNonNull(enrollmentPort, "enrollmentPort");
        this.labGroupPort = Objects.requireNonNull(labGroupPort, "labGroupPort");
        this.subjectPort = Objects.requireNonNull(subjectPort, "subjectPort");
        this.periodPort = Objects.requireNonNull(periodPort, "periodPort");
        this.integrityValidator = new AttendanceIntegrityValidator();
        this.authorizationService = authorizationService;
    }

    /**
     * Atomically records roll-call attendance for a session.
     * Enforces idempotency on duplicate submission:
     * - Identical payload: Returns existing roll-call summary (HTTP 200 Idempotent Success).
     * - Different payload on CONDUCTED session: Throws SessionStateConflictException (HTTP 409 Conflict).
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public SessionAttendanceSummaryResponse recordAttendance(UUID sessionId, RecordAttendanceBatchRequest request) {
        Session session = sessionPort.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Session not found with ID: " + sessionId));

        if (authorizationService != null) {
            authorizationService.requireAttendanceRecordingAccess(session);
        }

        // 1. Check Idempotency if session is already CONDUCTED
        if (session.status() == SessionStatus.CONDUCTED) {
            List<AttendanceRecord> existingRecords = recordPort.findBySession(sessionId);
            Map<UUID, AttendanceStatus> existingMap = existingRecords.stream()
                .collect(Collectors.toMap(AttendanceRecord::studentId, AttendanceRecord::status));

            Map<UUID, AttendanceStatus> incomingMap = new HashMap<>();
            for (AttendanceRecordItemDto item : request.records()) {
                incomingMap.put(item.studentId(), AttendanceStatus.valueOf(item.status()));
            }

            if (existingMap.equals(incomingMap)) {
                // Idempotent retry: exact same payload returns existing result
                return buildSummaryResponse(session, existingRecords);
            } else {
                throw new SessionStateConflictException("SESSION_ALREADY_CONDUCTED");
            }
        }

        if (session.status() == SessionStatus.CANCELLED || session.status() == SessionStatus.RESCHEDULED) {
            throw new InvalidSessionTransitionException(
                "Cannot record attendance on a " + session.status() + " session.");
        }

        // 2. Check for duplicate students in the request payload
        Set<UUID> seenStudents = new HashSet<>();
        for (AttendanceRecordItemDto item : request.records()) {
            if (!seenStudents.add(item.studentId())) {
                throw new InvalidBusinessOperationException(
                    "Duplicate student ID in attendance batch: " + item.studentId());
            }
        }

        // 3. Verify student enrollments and lab group eligibility
        List<Enrollment> enrollments = new ArrayList<>();
        for (AttendanceRecordItemDto item : request.records()) {
            Enrollment enrollment = enrollmentPort.findActiveEnrollment(item.studentId(), session.sectionId())
                .orElseThrow(() -> new StudentNotEligibleException(
                    "Student [%s] has no active enrollment in section [%s]"
                        .formatted(item.studentId(), session.sectionId())));

            if (!enrollment.isActiveOn(session.sessionDate())) {
                throw new StudentNotEligibleException(
                    "Student [%s] enrollment is not active on session date [%s]"
                        .formatted(item.studentId(), session.sessionDate()));
            }

            if (session.labGroupId().isPresent()) {
                if (enrollment.labGroupId().isEmpty() ||
                    !enrollment.labGroupId().get().equals(session.labGroupId().get())) {
                    throw new StudentNotEligibleException(
                        "Student [%s] does not belong to lab group [%s] for this laboratory session"
                            .formatted(item.studentId(), session.labGroupId().get()));
                }
            }
            enrollments.add(enrollment);
        }

        // 4. Construct domain AttendanceRecord objects
        List<AttendanceRecord> domainRecords = new ArrayList<>();
        for (AttendanceRecordItemDto item : request.records()) {
            domainRecords.add(new AttendanceRecord(
                UUID.randomUUID(),
                sessionId,
                item.studentId(),
                AttendanceStatus.valueOf(item.status())
            ));
        }

        // 5. Run pure domain integrity validator for each student
        com.amcs.domain.academic.Subject subject = subjectPort.findById(session.subjectId())
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + session.subjectId()));

        Session sessionAsConducted = new Session(
            session.id(),
            session.subjectId(),
            session.sectionId(),
            session.conductedByFacultyId(),
            session.sessionDate(),
            session.sessionType(),
            session.plannedUnits(),
            session.plannedUnits(),
            SessionStatus.CONDUCTED,
            session.labGroupId(),
            session.replacedBySessionId()
        );

        for (int i = 0; i < enrollments.size(); i++) {
            Enrollment enrollment = enrollments.get(i);
            AttendanceRecord record = domainRecords.get(i);
            AttendanceIntegrityReport report = integrityValidator.validate(
                record.studentId(),
                subject,
                new com.amcs.domain.academic.AcademicPeriod("Period", session.sessionDate().minusMonths(1), session.sessionDate().plusMonths(3)),
                enrollment,
                List.of(sessionAsConducted),
                List.of(record)
            );
            if (!report.isValid()) {
                throw new com.amcs.domain.attendance.AttendanceIntegrityException(report);
            }
        }

        // 6. Persist records atomically
        List<AttendanceRecord> savedRecords = recordPort.saveAll(domainRecords);

        // 7. Transition session to CONDUCTED with conductedUnits = plannedUnits
        Session conductedSession = sessionAsConducted;
        sessionPort.save(conductedSession, UUID.randomUUID());

        return buildSummaryResponse(conductedSession, savedRecords);
    }

    @Transactional(rollbackFor = Exception.class)
    public AttendanceCorrectionResponse correctAttendanceRecord(UUID recordId, CorrectAttendanceRecordRequest request) {
        AttendanceRecord record = recordPort.findById(recordId)
            .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found with ID: " + recordId));

        Session session = sessionPort.findById(record.sessionId())
            .orElseThrow(() -> new ResourceNotFoundException("Parent session not found: " + record.sessionId()));

        if (authorizationService != null) {
            authorizationService.requireAttendanceCorrectionAccess(session);
        }

        if (session.status() != SessionStatus.CONDUCTED) {
            throw new InvalidBusinessOperationException(
                "Cannot correct attendance for a session that is not in CONDUCTED state.");
        }

        AttendanceStatus newStatus = AttendanceStatus.valueOf(request.newStatus());
        if (record.status() == newStatus) {
            throw new InvalidBusinessOperationException("New status must differ from current status: " + newStatus);
        }

        AttendanceStatus oldStatus = record.status();
        AttendanceRecord corrected = new AttendanceRecord(record.id(), record.sessionId(), record.studentId(), newStatus);
        recordPort.save(corrected);

        return new AttendanceCorrectionResponse(
            record.id(),
            session.id(),
            record.studentId(),
            oldStatus.name(),
            newStatus.name(),
            request.reason().trim(),
            request.approverId(),
            Instant.now()
        );
    }

    public SessionAttendanceSummaryResponse getSessionAttendance(UUID sessionId) {
        Session session = sessionPort.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Session not found with ID: " + sessionId));
        if (authorizationService != null) {
            authorizationService.requireAttendanceRecordingAccess(session);
        }
        List<AttendanceRecord> records = recordPort.findBySession(sessionId);
        return buildSummaryResponse(session, records);
    }

    private SessionAttendanceSummaryResponse buildSummaryResponse(Session session, List<AttendanceRecord> records) {
        int presentCount = 0;
        int absentCount = 0;
        int otherCount = 0;

        List<AttendanceRecordResponse> recordResponses = new ArrayList<>();
        for (AttendanceRecord r : records) {
            if (r.status() == AttendanceStatus.PRESENT) presentCount++;
            else if (r.status() == AttendanceStatus.ABSENT) absentCount++;
            else otherCount++;

            recordResponses.add(new AttendanceRecordResponse(
                r.id(), r.sessionId(), r.studentId(), r.status().name(), Instant.now(), Instant.now()));
        }

        return new SessionAttendanceSummaryResponse(
            session.id(),
            session.subjectId(),
            session.sectionId(),
            session.sessionDate(),
            session.sessionType().name(),
            session.conductedUnits(),
            session.status().name(),
            records.size(),
            presentCount,
            absentCount,
            otherCount,
            recordResponses
        );
    }
}
