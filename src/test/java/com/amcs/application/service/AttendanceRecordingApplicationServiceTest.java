package com.amcs.application.service;

import com.amcs.application.dto.attendance.AttendanceCorrectionResponse;
import com.amcs.application.dto.attendance.AttendanceRecordItemDto;
import com.amcs.application.dto.attendance.CorrectAttendanceRecordRequest;
import com.amcs.application.dto.attendance.RecordAttendanceBatchRequest;
import com.amcs.application.dto.attendance.SessionAttendanceSummaryResponse;
import com.amcs.application.exception.InvalidBusinessOperationException;
import com.amcs.application.exception.InvalidSessionTransitionException;
import com.amcs.application.exception.SessionStateConflictException;
import com.amcs.application.exception.StudentNotEligibleException;
import com.amcs.application.port.out.AcademicPeriodRepositoryPort;
import com.amcs.application.port.out.AttendanceRecordRepositoryPort;
import com.amcs.application.port.out.EnrollmentRepositoryPort;
import com.amcs.application.port.out.LabGroupRepositoryPort;
import com.amcs.application.port.out.SessionRepositoryPort;
import com.amcs.application.port.out.SubjectRepositoryPort;
import com.amcs.domain.academic.CourseType;
import com.amcs.domain.academic.Subject;
import com.amcs.domain.attendance.AttendanceRecord;
import com.amcs.domain.attendance.AttendanceStatus;
import com.amcs.domain.attendance.Session;
import com.amcs.domain.attendance.SessionStatus;
import com.amcs.domain.attendance.SessionType;
import com.amcs.domain.enrollment.Enrollment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceRecordingApplicationServiceTest {

    @Mock private SessionRepositoryPort sessionPort;
    @Mock private AttendanceRecordRepositoryPort recordPort;
    @Mock private EnrollmentRepositoryPort enrollmentPort;
    @Mock private LabGroupRepositoryPort labGroupPort;
    @Mock private SubjectRepositoryPort subjectPort;
    @Mock private AcademicPeriodRepositoryPort periodPort;
    @Mock private com.amcs.application.security.ApplicationAuthorizationService authorizationService;

    private AttendanceRecordingApplicationService service;

    private final UUID sessionId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();
    private final UUID sectionId = UUID.randomUUID();
    private final UUID facultyId = UUID.randomUUID();
    private final UUID student1Id = UUID.randomUUID();
    private final UUID student2Id = UUID.randomUUID();
    private final LocalDate sessionDate = LocalDate.of(2026, 9, 1);

    private Session scheduledSession;
    private Subject testSubject;

    @BeforeEach
    void setUp() {
        service = new AttendanceRecordingApplicationService(
            sessionPort, recordPort, enrollmentPort, labGroupPort, subjectPort, periodPort, authorizationService);

        scheduledSession = new Session(
            sessionId,
            subjectId,
            sectionId,
            facultyId,
            sessionDate,
            SessionType.THEORY,
            1,
            0,
            SessionStatus.SCHEDULED,
            Optional.empty(),
            Optional.empty()
        );

        testSubject = new Subject(
            subjectId, "Data Structures", "CS201", CourseType.THEORY, 3);
    }

    @Nested
    @DisplayName("Roll-Call Batch Recording")
    class BatchRecordingTests {

        @Test
        @DisplayName("Should successfully record attendance and transition session to CONDUCTED")
        void shouldRecordAttendanceSuccessfully() {
            RecordAttendanceBatchRequest request = new RecordAttendanceBatchRequest(List.of(
                new AttendanceRecordItemDto(student1Id, "PRESENT"),
                new AttendanceRecordItemDto(student2Id, "ABSENT")
            ));

            Enrollment e1 = new Enrollment(student1Id, sectionId, LocalDate.of(2026, 8, 1), Optional.empty(), Optional.empty());
            Enrollment e2 = new Enrollment(student2Id, sectionId, LocalDate.of(2026, 8, 1), Optional.empty(), Optional.empty());

            when(sessionPort.findById(sessionId)).thenReturn(Optional.of(scheduledSession));
            when(enrollmentPort.findActiveEnrollment(student1Id, sectionId)).thenReturn(Optional.of(e1));
            when(enrollmentPort.findActiveEnrollment(student2Id, sectionId)).thenReturn(Optional.of(e2));
            when(subjectPort.findById(subjectId)).thenReturn(Optional.of(testSubject));
            when(recordPort.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            SessionAttendanceSummaryResponse response = service.recordAttendance(sessionId, request);

            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo("CONDUCTED");
            assertThat(response.totalRecords()).isEqualTo(2);
            assertThat(response.presentCount()).isEqualTo(1);
            assertThat(response.absentCount()).isEqualTo(1);
            verify(sessionPort).save(any(Session.class), any(UUID.class));
            verify(recordPort).saveAll(any());
        }

        @Test
        @DisplayName("Idempotent retry with exact same records returns 200 summary")
        void shouldReturnExistingSummaryOnIdenticalRetry() {
            Session conductedSession = new Session(
                sessionId, subjectId, sectionId, facultyId, sessionDate,
                SessionType.THEORY, 1, 1, SessionStatus.CONDUCTED, Optional.empty(), Optional.empty());

            AttendanceRecord r1 = new AttendanceRecord(UUID.randomUUID(), sessionId, student1Id, AttendanceStatus.PRESENT);
            AttendanceRecord r2 = new AttendanceRecord(UUID.randomUUID(), sessionId, student2Id, AttendanceStatus.ABSENT);

            RecordAttendanceBatchRequest request = new RecordAttendanceBatchRequest(List.of(
                new AttendanceRecordItemDto(student1Id, "PRESENT"),
                new AttendanceRecordItemDto(student2Id, "ABSENT")
            ));

            when(sessionPort.findById(sessionId)).thenReturn(Optional.of(conductedSession));
            when(recordPort.findBySession(sessionId)).thenReturn(List.of(r1, r2));

            SessionAttendanceSummaryResponse response = service.recordAttendance(sessionId, request);

            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo("CONDUCTED");
            assertThat(response.totalRecords()).isEqualTo(2);
        }

        @Test
        @DisplayName("Retry on CONDUCTED session with different records throws SessionStateConflictException (409)")
        void shouldThrowConflictWhenRetryingWithDifferentRecords() {
            Session conductedSession = new Session(
                sessionId, subjectId, sectionId, facultyId, sessionDate,
                SessionType.THEORY, 1, 1, SessionStatus.CONDUCTED, Optional.empty(), Optional.empty());

            AttendanceRecord r1 = new AttendanceRecord(UUID.randomUUID(), sessionId, student1Id, AttendanceStatus.PRESENT);
            AttendanceRecord r2 = new AttendanceRecord(UUID.randomUUID(), sessionId, student2Id, AttendanceStatus.ABSENT);

            RecordAttendanceBatchRequest differentRequest = new RecordAttendanceBatchRequest(List.of(
                new AttendanceRecordItemDto(student1Id, "ABSENT"), // altered status
                new AttendanceRecordItemDto(student2Id, "ABSENT")
            ));

            when(sessionPort.findById(sessionId)).thenReturn(Optional.of(conductedSession));
            when(recordPort.findBySession(sessionId)).thenReturn(List.of(r1, r2));

            assertThatThrownBy(() -> service.recordAttendance(sessionId, differentRequest))
                .isInstanceOf(SessionStateConflictException.class)
                .hasMessageContaining("SESSION_ALREADY_CONDUCTED");
        }

        @Test
        @DisplayName("Recording on CANCELLED session throws InvalidSessionTransitionException")
        void shouldThrowWhenRecordingOnCancelledSession() {
            Session cancelledSession = new Session(
                sessionId, subjectId, sectionId, facultyId, sessionDate,
                SessionType.THEORY, 1, 0, SessionStatus.CANCELLED, Optional.empty(), Optional.empty());

            RecordAttendanceBatchRequest request = new RecordAttendanceBatchRequest(List.of(
                new AttendanceRecordItemDto(student1Id, "PRESENT")
            ));

            when(sessionPort.findById(sessionId)).thenReturn(Optional.of(cancelledSession));

            assertThatThrownBy(() -> service.recordAttendance(sessionId, request))
                .isInstanceOf(InvalidSessionTransitionException.class)
                .hasMessageContaining("CANCELLED");
        }

        @Test
        @DisplayName("Duplicate student within batch request throws InvalidBusinessOperationException")
        void shouldThrowWhenDuplicateStudentInBatch() {
            RecordAttendanceBatchRequest duplicateBatch = new RecordAttendanceBatchRequest(List.of(
                new AttendanceRecordItemDto(student1Id, "PRESENT"),
                new AttendanceRecordItemDto(student1Id, "ABSENT")
            ));

            when(sessionPort.findById(sessionId)).thenReturn(Optional.of(scheduledSession));

            assertThatThrownBy(() -> service.recordAttendance(sessionId, duplicateBatch))
                .isInstanceOf(InvalidBusinessOperationException.class)
                .hasMessageContaining("Duplicate student ID");
        }

        @Test
        @DisplayName("Student not enrolled in section throws StudentNotEligibleException")
        void shouldThrowWhenStudentNotEnrolled() {
            RecordAttendanceBatchRequest request = new RecordAttendanceBatchRequest(List.of(
                new AttendanceRecordItemDto(student1Id, "PRESENT")
            ));

            when(sessionPort.findById(sessionId)).thenReturn(Optional.of(scheduledSession));
            when(enrollmentPort.findActiveEnrollment(student1Id, sectionId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.recordAttendance(sessionId, request))
                .isInstanceOf(StudentNotEligibleException.class)
                .hasMessageContaining("no active enrollment");
        }

        @Test
        @DisplayName("Lab group mismatch for lab session throws StudentNotEligibleException")
        void shouldThrowWhenLabGroupMismatch() {
            UUID labGroupId = UUID.randomUUID();
            UUID otherLabGroupId = UUID.randomUUID();

            Session labSession = new Session(
                sessionId, subjectId, sectionId, facultyId, sessionDate,
                SessionType.LAB, 2, 0, SessionStatus.SCHEDULED, Optional.of(labGroupId), Optional.empty());

            RecordAttendanceBatchRequest request = new RecordAttendanceBatchRequest(List.of(
                new AttendanceRecordItemDto(student1Id, "PRESENT")
            ));

            Enrollment wrongGroupEnrollment = new Enrollment(
                student1Id, sectionId, LocalDate.of(2026, 8, 1), Optional.empty(), Optional.of(otherLabGroupId));

            when(sessionPort.findById(sessionId)).thenReturn(Optional.of(labSession));
            when(enrollmentPort.findActiveEnrollment(student1Id, sectionId)).thenReturn(Optional.of(wrongGroupEnrollment));

            assertThatThrownBy(() -> service.recordAttendance(sessionId, request))
                .isInstanceOf(StudentNotEligibleException.class)
                .hasMessageContaining("does not belong to lab group");
        }
    }

    @Nested
    @DisplayName("Attendance Correction")
    class CorrectionTests {

        @Test
        @DisplayName("Successful correction returns audited correction response")
        void shouldCorrectAttendanceRecord() {
            UUID recordId = UUID.randomUUID();
            UUID approverId = UUID.randomUUID();

            Session conductedSession = new Session(
                sessionId, subjectId, sectionId, facultyId, sessionDate,
                SessionType.THEORY, 1, 1, SessionStatus.CONDUCTED, Optional.empty(), Optional.empty());

            AttendanceRecord existingRecord = new AttendanceRecord(
                recordId, sessionId, student1Id, AttendanceStatus.ABSENT);

            CorrectAttendanceRecordRequest request = new CorrectAttendanceRecordRequest(
                "PRESENT", "Student was present at seminar with permission slip", approverId);

            when(recordPort.findById(recordId)).thenReturn(Optional.of(existingRecord));
            when(sessionPort.findById(sessionId)).thenReturn(Optional.of(conductedSession));

            AttendanceCorrectionResponse response = service.correctAttendanceRecord(recordId, request);

            assertThat(response).isNotNull();
            assertThat(response.previousStatus()).isEqualTo("ABSENT");
            assertThat(response.newStatus()).isEqualTo("PRESENT");
            assertThat(response.reason()).isEqualTo(request.reason());
            verify(recordPort).save(any(AttendanceRecord.class));
        }

        @Test
        @DisplayName("Correction with identical status throws InvalidBusinessOperationException")
        void shouldThrowWhenCorrectionStatusIsIdentical() {
            UUID recordId = UUID.randomUUID();
            AttendanceRecord existingRecord = new AttendanceRecord(
                recordId, sessionId, student1Id, AttendanceStatus.PRESENT);

            Session conductedSession = new Session(
                sessionId, subjectId, sectionId, facultyId, sessionDate,
                SessionType.THEORY, 1, 1, SessionStatus.CONDUCTED, Optional.empty(), Optional.empty());

            CorrectAttendanceRecordRequest request = new CorrectAttendanceRecordRequest(
                "PRESENT", "No actual change", UUID.randomUUID());

            when(recordPort.findById(recordId)).thenReturn(Optional.of(existingRecord));
            when(sessionPort.findById(sessionId)).thenReturn(Optional.of(conductedSession));

            assertThatThrownBy(() -> service.correctAttendanceRecord(recordId, request))
                .isInstanceOf(InvalidBusinessOperationException.class)
                .hasMessageContaining("New status must differ");
        }
    }
}
