package com.amcs.application.service.importer.validator;

import com.amcs.application.port.out.AttendanceRecordRepositoryPort;
import com.amcs.application.port.out.EnrollmentRepositoryPort;
import com.amcs.application.port.out.SessionRepositoryPort;
import com.amcs.application.port.out.StudentRepositoryPort;
import com.amcs.application.port.out.excel.ConversionResult;
import com.amcs.application.port.out.excel.ParsedRow;
import com.amcs.application.port.out.security.AuthenticatedActor;
import com.amcs.application.port.out.security.FacultyAssignmentRepositoryPort;
import com.amcs.application.service.importer.payload.StagedAttendancePayload;
import com.amcs.domain.attendance.AttendanceStatus;
import com.amcs.domain.attendance.Session;
import com.amcs.domain.importer.RowValidationError;
import com.amcs.infrastructure.excel.converter.CellTypeConverter;
import com.amcs.infrastructure.persistence.entity.StudentEntity;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class AttendanceRowValidator {

    private final SessionRepositoryPort sessionRepository;
    private final StudentRepositoryPort studentRepository;
    private final EnrollmentRepositoryPort enrollmentRepository;
    private final AttendanceRecordRepositoryPort attendanceRecordRepository;
    private final FacultyAssignmentRepositoryPort facultyAssignmentRepository;

    public AttendanceRowValidator(
        SessionRepositoryPort sessionRepository,
        StudentRepositoryPort studentRepository,
        EnrollmentRepositoryPort enrollmentRepository,
        AttendanceRecordRepositoryPort attendanceRecordRepository,
        FacultyAssignmentRepositoryPort facultyAssignmentRepository
    ) {
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "sessionRepository");
        this.studentRepository = Objects.requireNonNull(studentRepository, "studentRepository");
        this.enrollmentRepository = Objects.requireNonNull(enrollmentRepository, "enrollmentRepository");
        this.attendanceRecordRepository = Objects.requireNonNull(attendanceRecordRepository, "attendanceRecordRepository");
        this.facultyAssignmentRepository = Objects.requireNonNull(facultyAssignmentRepository, "facultyAssignmentRepository");
    }

    public ValidationOutcome<StagedAttendancePayload> validateRow(
        ParsedRow row,
        Set<String> seenAttendance,
        AuthenticatedActor actor
    ) {
        int rowIndex = row.rowIndex();

        // Level 1: Syntactic
        String sessionRaw = row.get("Session Identifier");
        if (sessionRaw.isEmpty()) {
            return ValidationOutcome.invalid(new RowValidationError(
                rowIndex, "Session Identifier", "", "MISSING_REQUIRED_FIELD", "Session Identifier is required"
            ));
        }

        String studentRaw = row.get("Student Identifier");
        if (studentRaw.isEmpty()) {
            return ValidationOutcome.invalid(new RowValidationError(
                rowIndex, "Student Identifier", "", "MISSING_REQUIRED_FIELD", "Student Identifier is required"
            ));
        }

        ConversionResult<AttendanceStatus> statusResult = CellTypeConverter.asEnum(
            AttendanceStatus.class, row.get("Attendance Status"), "Attendance Status", rowIndex, true
        );
        if (!statusResult.isSuccess()) {
            return ValidationOutcome.invalid(statusResult.getError().get());
        }
        AttendanceStatus status = statusResult.getValue().orElseThrow();

        // Level 2: Referential
        UUID sessionId;
        try {
            sessionId = UUID.fromString(sessionRaw);
        } catch (IllegalArgumentException e) {
            return ValidationOutcome.invalid(new RowValidationError(
                rowIndex, "Session Identifier", sessionRaw, "INVALID_UUID", "Session Identifier must be a valid UUID"
            ));
        }

        Optional<Session> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            return ValidationOutcome.invalid(new RowValidationError(
                rowIndex, "Session Identifier", sessionRaw, "SESSION_NOT_FOUND", "Session not found with ID: '" + sessionRaw + "'"
            ));
        }
        Session session = sessionOpt.get();

        // Resolve student (either by registration number or UUID)
        Optional<StudentEntity> studentOpt = studentRepository.findByRegistrationNumber(studentRaw);
        if (studentOpt.isEmpty()) {
            try {
                UUID studentId = UUID.fromString(studentRaw);
                studentOpt = studentRepository.findById(studentId);
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (studentOpt.isEmpty()) {
            return ValidationOutcome.invalid(new RowValidationError(
                rowIndex, "Student Identifier", studentRaw, "STUDENT_NOT_FOUND", "Student not found: '" + studentRaw + "'"
            ));
        }
        StudentEntity student = studentOpt.get();

        // Level 3: Business & Scope
        // Enrollment check
        boolean isEnrolled = enrollmentRepository.findActiveEnrollment(student.getId(), session.sectionId()).isPresent();
        if (!isEnrolled) {
            return ValidationOutcome.invalid(new RowValidationError(
                rowIndex, "Student Identifier", studentRaw, "STUDENT_NOT_ENROLLED",
                "Student '" + studentRaw + "' is not actively enrolled in the section for session '" + sessionRaw + "'"
            ));
        }

        // Faculty scope check
        if (actor.isFaculty()) {
            UUID facultyId = actor.facultyId().orElse(null);
            boolean isConductor = session.conductedByFacultyId().equals(facultyId);
            boolean isAssigned = facultyAssignmentRepository.findByFacultyId(facultyId).stream()
                .anyMatch(a -> a.subjectId().equals(session.subjectId()) && a.sectionId().equals(session.sectionId()));

            if (!isConductor && !isAssigned) {
                return ValidationOutcome.invalid(new RowValidationError(
                    rowIndex, "Session Identifier", sessionRaw, "FACULTY_NOT_AUTHORIZED",
                    "Faculty is not authorized to record attendance for this session"
                ));
            }
        }

        // File duplicate check
        String attendanceKey = session.id() + ":" + student.getId();
        if (seenAttendance.contains(attendanceKey)) {
            return ValidationOutcome.invalid(new RowValidationError(
                rowIndex, "Student Identifier", studentRaw, "DUPLICATE_IN_FILE",
                "Duplicate attendance record in file for session '" + sessionRaw + "' and student '" + studentRaw + "'"
            ));
        }
        seenAttendance.add(attendanceKey);

        // Database duplicate check
        if (attendanceRecordRepository.findBySessionAndStudent(session.id(), student.getId()).isPresent()) {
            return ValidationOutcome.invalid(new RowValidationError(
                rowIndex, "Student Identifier", studentRaw, "DUPLICATE_ATTENDANCE",
                "Attendance has already been recorded for student '" + studentRaw + "' in session '" + sessionRaw + "'"
            ));
        }

        return ValidationOutcome.valid(new StagedAttendancePayload(
            session.id(), student.getId(), status
        ));
    }
}
