package com.amcs.domain.policy;

/**
 * Defines how the calculation engine handles a conducted session for which
 * an enrolled student has NO attendance record.
 *
 * <p><strong>CRITICAL ARCHITECTURAL DECISION:</strong> Missing attendance records must
 * NOT be unconditionally treated as absences. The handling of missing data is an
 * institutional policy decision and must be configurable.
 *
 * <p>Supported strategies:
 * <ul>
 *   <li>{@link #TREAT_AS_ABSENT}: The missing session contributes to the conducted denominator,
 *       and the student receives zero attendance units (or the policy's ABSENT contribution).</li>
 *   <li>{@link #EXCLUDE_FROM_CALCULATION}: The session is omitted entirely from this student's
 *       calculation (neither numerator nor denominator is incremented).</li>
 *   <li>{@link #MARK_AS_INCOMPLETE}: The calculation result is flagged as INCOMPLETE.
 *       The student cannot be classified as ADEQUATE or SHORTAGE until the missing data
 *       is administratively resolved.</li>
 * </ul>
 *
 * <p>REQUIRES INSTITUTIONAL CONFIRMATION (RIC-MR-001): Which strategy is the institutional default?
 */
public enum MissingRecordStrategy {

    /**
     * Missing record is treated as an absence.
     * Denominator includes the session; numerator receives zero (or ABSENT contribution).
     */
    TREAT_AS_ABSENT,

    /**
     * Session with missing record is omitted entirely from this student's calculation.
     * Denominator does NOT include the session; numerator does NOT include the session.
     */
    EXCLUDE_FROM_CALCULATION,

    /**
     * Calculation is flagged as incomplete.
     * The classification becomes {@link com.amcs.domain.attendance.AttendanceClassification#INCOMPLETE},
     * signaling that administrative or faculty data entry is pending.
     */
    MARK_AS_INCOMPLETE
}
