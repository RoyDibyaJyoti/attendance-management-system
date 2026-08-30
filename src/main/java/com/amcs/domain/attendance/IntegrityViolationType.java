package com.amcs.domain.attendance;

/**
 * Classifies data corruption or business invariant violations in attendance data.
 */
public enum IntegrityViolationType {

    /** Student was not actively enrolled on the session date. */
    STUDENT_NOT_ENROLLED,

    /** Attendance was recorded for a session belonging to a different lab group. */
    WRONG_LAB_GROUP,

    /** Multiple attendance records exist for the same student and session. */
    DUPLICATE_RECORD,

    /** Attendance was recorded for a session that was not CONDUCTED (e.g., CANCELLED or SCHEDULED). */
    SESSION_NOT_CONDUCTED,

    /** Attendance was recorded for a session occurring outside the academic period. */
    OUTSIDE_ACADEMIC_PERIOD,

    /** Attendance record points to a session belonging to a different subject. */
    SUBJECT_MISMATCH,

    /** Attendance record points to a session ID that does not exist in the session registry. */
    UNKNOWN_SESSION
}
