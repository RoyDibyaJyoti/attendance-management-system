package com.amcs.domain.attendance;

import java.util.Objects;
import java.util.UUID;

/**
 * Records a single student's attendance status for a single session.
 *
 * <p>Domain constraints:
 * <ul>
 *   <li>One record per (sessionId, studentId) — enforced by DB unique constraint at persistence layer.</li>
 *   <li>A record MUST NOT exist for a student not enrolled in the subject-section for the session date
 *       (enforced at the service/recording layer, not here).</li>
 *   <li>If a CONDUCTED session has no record for a student, the calculation engine treats it as ABSENT
 *       and flags it as a data quality issue. See SubjectAttendanceCalculator.</li>
 * </ul>
 */
public record AttendanceRecord(
    UUID id,
    UUID sessionId,
    UUID studentId,
    AttendanceStatus status
) {
    public AttendanceRecord {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(studentId, "studentId must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}
