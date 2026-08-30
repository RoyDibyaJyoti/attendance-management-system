package com.amcs.application.service.importer.validator;

import com.amcs.application.port.out.AttendanceRecordRepositoryPort;
import com.amcs.application.port.out.EnrollmentRepositoryPort;
import com.amcs.application.port.out.SessionRepositoryPort;
import com.amcs.application.port.out.StudentRepositoryPort;
import com.amcs.application.port.out.excel.ParsedRow;
import com.amcs.application.port.out.security.AuthenticatedActor;
import com.amcs.application.port.out.security.FacultyAssignmentRepositoryPort;
import com.amcs.application.port.out.security.UserRole;
import com.amcs.application.service.importer.payload.StagedAttendancePayload;
import com.amcs.domain.attendance.AttendanceRecord;
import com.amcs.domain.attendance.AttendanceStatus;
import com.amcs.domain.attendance.Session;
import com.amcs.domain.attendance.SessionStatus;
import com.amcs.domain.attendance.SessionType;
import com.amcs.domain.enrollment.Enrollment;
import com.amcs.infrastructure.persistence.entity.StudentEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceRowValidator Unit Tests")
class AttendanceRowValidatorTest {

    @Mock private SessionRepositoryPort sessionRepository;
    @Mock private StudentRepositoryPort studentRepository;
    @Mock private EnrollmentRepositoryPort enrollmentRepository;
    @Mock private AttendanceRecordRepositoryPort attendanceRecordRepository;
    @Mock private FacultyAssignmentRepositoryPort facultyAssignmentRepository;

    private AttendanceRowValidator validator;

    private final UUID sessionId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID sectionId = UUID.randomUUID();
    private final UUID facultyId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private Set<String> seenAttendance;

    private final AuthenticatedActor adminActor = new AuthenticatedActor(
        userId, "admin", UserRole.HOD_ADMIN, Optional.empty(), Optional.empty()
    );

    @BeforeEach
    void setUp() {
        validator = new AttendanceRowValidator(
            sessionRepository, studentRepository, enrollmentRepository,
            attendanceRecordRepository, facultyAssignmentRepository
        );
        seenAttendance = new HashSet<>();
    }

    private ParsedRow createRow(String sessId, String studId, String status) {
        Map<String, String> map = Map.of(
            "session identifier", sessId,
            "student identifier", studId,
            "attendance status", status
        );
        return new ParsedRow(2, map, List.of(sessId, studId, status), false);
    }

    @Test
    @DisplayName("Valid attendance row passes and returns staged payload")
    void shouldValidateCleanAttendanceRow() {
        ParsedRow row = createRow(sessionId.toString(), "CS2026-001", "PRESENT");

        Session session = new Session(
            sessionId, UUID.randomUUID(), sectionId, facultyId,
            LocalDate.of(2026, 9, 10), SessionType.THEORY, 1, 0, SessionStatus.SCHEDULED,
            Optional.empty(), Optional.empty()
        );
        StudentEntity student = new StudentEntity(studentId, "CS2026-001", "Alice", "alice@univ.edu", UUID.randomUUID());
        Enrollment enrollment = new Enrollment(studentId, sectionId, LocalDate.of(2026, 9, 1), Optional.empty(), Optional.empty());

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(studentRepository.findByRegistrationNumber("CS2026-001")).thenReturn(Optional.of(student));
        when(enrollmentRepository.findActiveEnrollment(studentId, sectionId)).thenReturn(Optional.of(enrollment));
        when(attendanceRecordRepository.findBySessionAndStudent(sessionId, studentId)).thenReturn(Optional.empty());

        ValidationOutcome<StagedAttendancePayload> outcome = validator.validateRow(row, seenAttendance, adminActor);

        assertThat(outcome.isValid()).isTrue();
        StagedAttendancePayload payload = outcome.getPayload().orElseThrow();
        assertThat(payload.sessionId()).isEqualTo(sessionId);
        assertThat(payload.studentId()).isEqualTo(studentId);
        assertThat(payload.status()).isEqualTo(AttendanceStatus.PRESENT);
    }

    @Test
    @DisplayName("Student not enrolled in section is rejected")
    void shouldRejectUnenrolledStudent() {
        ParsedRow row = createRow(sessionId.toString(), "CS2026-002", "PRESENT");

        Session session = new Session(
            sessionId, UUID.randomUUID(), sectionId, facultyId,
            LocalDate.of(2026, 9, 10), SessionType.THEORY, 1, 0, SessionStatus.SCHEDULED,
            Optional.empty(), Optional.empty()
        );
        StudentEntity student = new StudentEntity(studentId, "CS2026-002", "Bob", "bob@univ.edu", UUID.randomUUID());

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(studentRepository.findByRegistrationNumber("CS2026-002")).thenReturn(Optional.of(student));
        when(enrollmentRepository.findActiveEnrollment(studentId, sectionId)).thenReturn(Optional.empty());

        ValidationOutcome<StagedAttendancePayload> outcome = validator.validateRow(row, seenAttendance, adminActor);

        assertThat(outcome.isValid()).isFalse();
        assertThat(outcome.getError().orElseThrow().errorCode()).isEqualTo("STUDENT_NOT_ENROLLED");
    }

    @Test
    @DisplayName("Duplicate attendance record already in DB is rejected")
    void shouldRejectDuplicateAttendanceInDb() {
        ParsedRow row = createRow(sessionId.toString(), "CS2026-003", "ABSENT");

        Session session = new Session(
            sessionId, UUID.randomUUID(), sectionId, facultyId,
            LocalDate.of(2026, 9, 10), SessionType.THEORY, 1, 0, SessionStatus.SCHEDULED,
            Optional.empty(), Optional.empty()
        );
        StudentEntity student = new StudentEntity(studentId, "CS2026-003", "Charlie", "charlie@univ.edu", UUID.randomUUID());
        Enrollment enrollment = new Enrollment(studentId, sectionId, LocalDate.of(2026, 9, 1), Optional.empty(), Optional.empty());
        AttendanceRecord existing = new AttendanceRecord(UUID.randomUUID(), sessionId, studentId, AttendanceStatus.PRESENT);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(studentRepository.findByRegistrationNumber("CS2026-003")).thenReturn(Optional.of(student));
        when(enrollmentRepository.findActiveEnrollment(studentId, sectionId)).thenReturn(Optional.of(enrollment));
        when(attendanceRecordRepository.findBySessionAndStudent(sessionId, studentId)).thenReturn(Optional.of(existing));

        ValidationOutcome<StagedAttendancePayload> outcome = validator.validateRow(row, seenAttendance, adminActor);

        assertThat(outcome.isValid()).isFalse();
        assertThat(outcome.getError().orElseThrow().errorCode()).isEqualTo("DUPLICATE_ATTENDANCE");
    }
}
