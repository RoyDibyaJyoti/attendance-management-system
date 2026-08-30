package com.amcs.domain.attendance;

import com.amcs.domain.academic.AcademicPeriod;
import com.amcs.domain.academic.Subject;
import com.amcs.domain.calculation.AttendanceCalculationEngine;
import com.amcs.domain.calculation.SubjectAttendanceCalculator;
import com.amcs.domain.enrollment.Enrollment;
import com.amcs.domain.test.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.amcs.domain.test.TestFixtures.*;
import static org.assertj.core.api.Assertions.*;

@DisplayName("AttendanceIntegrityValidator Tests")
class AttendanceIntegrityValidatorTest {

    private AttendanceIntegrityValidator validator;
    private AttendanceCalculationEngine engine;
    private SubjectAttendanceCalculator subjectCalculator;
    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        validator = new AttendanceIntegrityValidator();
        engine = new AttendanceCalculationEngine();
        subjectCalculator = new SubjectAttendanceCalculator();
        enrollment = fullEnrollment(STUDENT_1);
    }

    @Test
    @DisplayName("IV-01: Valid records produce clean report with zero violations")
    void validRecords_cleanReport() {
        UUID s1 = UUID.randomUUID();
        var sessions = List.of(conductedTheorySession(s1, semDay(1)));
        var records = List.of(record(s1, STUDENT_1, AttendanceStatus.PRESENT));

        AttendanceIntegrityReport report = validator.validate(
            STUDENT_1, theorySubject(), SEMESTER_1, enrollment, sessions, records);

        assertThat(report.isValid()).isTrue();
        assertThat(report.hasViolations()).isFalse();
        assertThat(report.violationCount()).isZero();
    }

    @Test
    @DisplayName("IV-02: Record for student not actively enrolled is flagged as STUDENT_NOT_ENROLLED")
    void unenrolledStudent_flagged() {
        UUID s1 = UUID.randomUUID();
        var sessions = List.of(conductedTheorySession(s1, semDay(1))); // day 1
        var records = List.of(record(s1, STUDENT_1, AttendanceStatus.PRESENT));

        // Enrollment starts on day 10 (after session date)
        var lateEnrollment = new Enrollment(
            STUDENT_1, SECTION_1, semDay(10), Optional.empty(), Optional.empty());

        AttendanceIntegrityReport report = validator.validate(
            STUDENT_1, theorySubject(), SEMESTER_1, lateEnrollment, sessions, records);

        assertThat(report.hasViolations()).isTrue();
        assertThat(report.hasViolationType(IntegrityViolationType.STUDENT_NOT_ENROLLED)).isTrue();
        assertThat(report.getViolationsForType(IntegrityViolationType.STUDENT_NOT_ENROLLED)).hasSize(1);
    }

    @Test
    @DisplayName("IV-03: Record for session in wrong lab group is flagged as WRONG_LAB_GROUP")
    void wrongLabGroup_flagged() {
        UUID s1 = UUID.randomUUID();
        // Session is assigned to LAB_GROUP_B
        var session = conductedLabSessionForGroup(s1, semDay(1), 3, LAB_GROUP_B);
        var records = List.of(record(s1, STUDENT_1, AttendanceStatus.PRESENT));

        // Student is enrolled in LAB_GROUP_A
        var groupAEnrollment = enrollmentWithLabGroup(STUDENT_1, LAB_GROUP_A);

        AttendanceIntegrityReport report = validator.validate(
            STUDENT_1, labSubject(), SEMESTER_1, groupAEnrollment, List.of(session), records);

        assertThat(report.hasViolationType(IntegrityViolationType.WRONG_LAB_GROUP)).isTrue();
    }

    @Test
    @DisplayName("IV-04: Duplicate records for same session and student are flagged as DUPLICATE_RECORD")
    void duplicateRecord_flagged() {
        UUID s1 = UUID.randomUUID();
        var session = conductedTheorySession(s1, semDay(1));
        var records = List.of(
            record(s1, STUDENT_1, AttendanceStatus.PRESENT),
            record(s1, STUDENT_1, AttendanceStatus.ABSENT) // duplicate
        );

        AttendanceIntegrityReport report = validator.validate(
            STUDENT_1, theorySubject(), SEMESTER_1, enrollment, List.of(session), records);

        assertThat(report.hasViolationType(IntegrityViolationType.DUPLICATE_RECORD)).isTrue();
    }

    @Test
    @DisplayName("IV-05: Attendance for CANCELLED session is flagged as SESSION_NOT_CONDUCTED")
    void cancelledSessionRecord_flagged() {
        UUID s1 = UUID.randomUUID();
        var cancelled = cancelledSession(s1, semDay(1));
        var records = List.of(record(s1, STUDENT_1, AttendanceStatus.PRESENT));

        AttendanceIntegrityReport report = validator.validate(
            STUDENT_1, theorySubject(), SEMESTER_1, enrollment, List.of(cancelled), records);

        assertThat(report.hasViolationType(IntegrityViolationType.SESSION_NOT_CONDUCTED)).isTrue();
    }

    @Test
    @DisplayName("IV-06: Session outside academic period is flagged as OUTSIDE_ACADEMIC_PERIOD")
    void outsideAcademicPeriod_flagged() {
        UUID s1 = UUID.randomUUID();
        LocalDate outsideDate = SEMESTER_1.endDate().plusDays(5);
        var session = new Session(
            s1, SUBJECT_THEORY, SECTION_1, FACULTY_1, outsideDate,
            SessionType.THEORY, 1, 1, SessionStatus.CONDUCTED,
            Optional.empty(), Optional.empty());
        var records = List.of(record(s1, STUDENT_1, AttendanceStatus.PRESENT));

        AttendanceIntegrityReport report = validator.validate(
            STUDENT_1, theorySubject(), SEMESTER_1, enrollment, List.of(session), records);

        assertThat(report.hasViolationType(IntegrityViolationType.OUTSIDE_ACADEMIC_PERIOD)).isTrue();
    }

    @Test
    @DisplayName("IV-07: Attendance record for different subject is flagged as SUBJECT_MISMATCH")
    void subjectMismatch_flagged() {
        UUID s1 = UUID.randomUUID();
        // Session belongs to labSubject, but validation is for theorySubject
        var session = conductedLabSession(s1, semDay(1), 1);
        var records = List.of(record(s1, STUDENT_1, AttendanceStatus.PRESENT));

        AttendanceIntegrityReport report = validator.validate(
            STUDENT_1, theorySubject(), SEMESTER_1, enrollment, List.of(session), records);

        assertThat(report.hasViolationType(IntegrityViolationType.SUBJECT_MISMATCH)).isTrue();
    }

    @Test
    @DisplayName("IV-08: Attendance record pointing to non-existent session is flagged as UNKNOWN_SESSION")
    void unknownSession_flagged() {
        UUID nonExistentSessionId = UUID.randomUUID();
        var records = List.of(record(nonExistentSessionId, STUDENT_1, AttendanceStatus.PRESENT));

        AttendanceIntegrityReport report = validator.validate(
            STUDENT_1, theorySubject(), SEMESTER_1, enrollment, List.of(), records);

        assertThat(report.hasViolationType(IntegrityViolationType.UNKNOWN_SESSION)).isTrue();
    }

    @Test
    @DisplayName("IV-09: calculateSubjectAttendanceStrict throws AttendanceIntegrityException when violations exist")
    void strictCalculation_throwsIntegrityException() {
        UUID s1 = UUID.randomUUID();
        var cancelled = cancelledSession(s1, semDay(1));
        var records = List.of(record(s1, STUDENT_1, AttendanceStatus.PRESENT));

        assertThatThrownBy(() -> engine.calculateSubjectAttendanceStrict(
            STUDENT_1, theorySubject(), policy75(), List.of(cancelled), records,
            enrollment, SEMESTER_1))
            .isInstanceOf(AttendanceIntegrityException.class)
            .satisfies(e -> {
                AttendanceIntegrityException ex = (AttendanceIntegrityException) e;
                assertThat(ex.getReport().hasViolationType(IntegrityViolationType.SESSION_NOT_CONDUCTED)).isTrue();
            });
    }
}
