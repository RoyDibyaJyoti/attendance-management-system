package com.amcs.domain.attendance;

import com.amcs.domain.academic.AcademicPeriod;
import com.amcs.domain.academic.Subject;
import com.amcs.domain.enrollment.Enrollment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Validates attendance dataset integrity prior to calculation.
 *
 * <p>Detects seven distinct data corruption / invariant violation scenarios:
 * <ul>
 *   <li>Attendance record for a student not actively enrolled on the session date</li>
 *   <li>Attendance record for a student assigned to a different lab group</li>
 *   <li>Duplicate attendance records for the same student and session</li>
 *   <li>Attendance record for a session that is CANCELLED, SCHEDULED, or RESCHEDULED</li>
 *   <li>Attendance record for a session dated outside the academic period</li>
 *   <li>Attendance record for a session associated with a different subject</li>
 *   <li>Attendance record pointing to a non-existent session ID</li>
 * </ul>
 */
public class AttendanceIntegrityValidator {

    /**
     * Validates a student's attendance records against the subject, enrollment, and sessions.
     *
     * @param studentId the student being checked
     * @param subject the subject context
     * @param period the academic period
     * @param enrollment the student's enrollment
     * @param sessions all available sessions
     * @param records the attendance records to inspect
     * @return an {@link AttendanceIntegrityReport} detailing all findings
     */
    public AttendanceIntegrityReport validate(
        UUID studentId,
        Subject subject,
        AcademicPeriod period,
        Enrollment enrollment,
        List<Session> sessions,
        List<AttendanceRecord> records
    ) {
        Objects.requireNonNull(studentId, "studentId must not be null");
        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(period, "period must not be null");
        Objects.requireNonNull(enrollment, "enrollment must not be null");
        Objects.requireNonNull(sessions, "sessions must not be null");
        Objects.requireNonNull(records, "records must not be null");

        List<IntegrityViolation> violations = new ArrayList<>();
        Map<UUID, Session> sessionById = sessions.stream()
            .collect(Collectors.toMap(Session::id, s -> s, (s1, s2) -> s1));

        Set<UUID> seenSessionIds = new HashSet<>();

        for (AttendanceRecord record : records) {
            // Only validate records intended for this student
            if (!studentId.equals(record.studentId())) {
                continue;
            }

            UUID sessionId = record.sessionId();

            // 1. Unknown session check
            Session session = sessionById.get(sessionId);
            if (session == null) {
                violations.add(new IntegrityViolation(
                    IntegrityViolationType.UNKNOWN_SESSION,
                    sessionId, studentId,
                    "AttendanceRecord references session ID [%s] which does not exist in sessions list"
                        .formatted(sessionId)));
                continue; // Cannot perform further session-level checks without the session
            }

            // 2. Duplicate record check
            if (!seenSessionIds.add(sessionId)) {
                violations.add(new IntegrityViolation(
                    IntegrityViolationType.DUPLICATE_RECORD,
                    sessionId, studentId,
                    "Duplicate AttendanceRecord found for student [%s] on session [%s]"
                        .formatted(studentId, sessionId)));
            }

            // 3. Subject mismatch check
            if (!subject.id().equals(session.subjectId())) {
                violations.add(new IntegrityViolation(
                    IntegrityViolationType.SUBJECT_MISMATCH,
                    sessionId, studentId,
                    "Session [%s] belongs to subject [%s], but validation expected subject [%s]"
                        .formatted(sessionId, session.subjectId(), subject.id())));
            }

            // 4. Session not conducted check
            if (session.status() != SessionStatus.CONDUCTED) {
                violations.add(new IntegrityViolation(
                    IntegrityViolationType.SESSION_NOT_CONDUCTED,
                    sessionId, studentId,
                    "Attendance recorded for non-CONDUCTED session [%s] (status: %s)"
                        .formatted(sessionId, session.status())));
            }

            // 5. Outside academic period check
            if (!period.contains(session.sessionDate())) {
                violations.add(new IntegrityViolation(
                    IntegrityViolationType.OUTSIDE_ACADEMIC_PERIOD,
                    sessionId, studentId,
                    "Session [%s] date [%s] is outside academic period [%s to %s]"
                        .formatted(sessionId, session.sessionDate(), period.startDate(), period.endDate())));
            }

            // 6. Student not actively enrolled check
            if (!enrollment.isActiveOn(session.sessionDate())) {
                violations.add(new IntegrityViolation(
                    IntegrityViolationType.STUDENT_NOT_ENROLLED,
                    sessionId, studentId,
                    "Student [%s] enrollment is not active on session date [%s] (start: %s, end: %s)"
                        .formatted(studentId, session.sessionDate(), enrollment.enrollmentStart(),
                            enrollment.enrollmentEnd().map(Object::toString).orElse("None"))));
            }

            // 7. Wrong lab group check
            if (!enrollment.isEligibleForLabGroup(session.labGroupId())) {
                violations.add(new IntegrityViolation(
                    IntegrityViolationType.WRONG_LAB_GROUP,
                    sessionId, studentId,
                    "Student [%s] in lab group [%s] is ineligible for session [%s] assigned to lab group [%s]"
                        .formatted(studentId, enrollment.labGroupId().map(Object::toString).orElse("None"),
                            sessionId, session.labGroupId().map(Object::toString).orElse("None"))));
            }
        }

        return new AttendanceIntegrityReport(violations);
    }
}
