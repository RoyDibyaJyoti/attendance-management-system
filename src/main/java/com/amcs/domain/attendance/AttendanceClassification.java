package com.amcs.domain.attendance;

/**
 * Classification of a student's attendance outcome for a given subject and period.
 */
public enum AttendanceClassification {

    /** Attendance percentage is at or above the minimum required threshold. */
    ADEQUATE,

    /** Attendance percentage is strictly below the minimum required threshold. */
    SHORTAGE,

    /**
     * No sessions have been conducted yet for this student in this period.
     * The denominator is zero; the percentage is undefined (not 0%).
     * MUST NOT be treated as shortage or as adequate.
     */
    UNDEFINED,

    /**
     * One or more conducted sessions have no attendance record for the student,
     * and the policy's MissingRecordStrategy is MARK_AS_INCOMPLETE.
     * Denotes that administrative or faculty data entry is pending before an
     * official shortage/adequate determination can be rendered.
     */
    INCOMPLETE
}
