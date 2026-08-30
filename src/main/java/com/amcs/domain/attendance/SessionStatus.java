package com.amcs.domain.attendance;

/**
 * Status of a class or lab session.
 *
 * <p>Only {@link #CONDUCTED} sessions contribute to the attendance denominator.
 * All other statuses MUST be excluded from the denominator calculation.
 * This is a CONFIRMED rule (DOMAIN_RULES.md Rule S-004).
 */
public enum SessionStatus {

    /** Session is planned but has not occurred yet. Does NOT count. */
    SCHEDULED,

    /**
     * Session was actually held. Attendance records exist for this session.
     * This is the ONLY status that contributes to the attendance denominator.
     */
    CONDUCTED,

    /**
     * Session was cancelled. MUST NOT be included in the denominator.
     * Retroactive cancellation of a CONDUCTED session is a special case —
     * see DOMAIN_RULES.md Rule RC-003 (REQUIRES INSTITUTIONAL CONFIRMATION).
     */
    CANCELLED,

    /**
     * Session was rescheduled. The original session is marked RESCHEDULED.
     * A new session is created as the replacement.
     * A RESCHEDULED session MUST NOT contribute to the denominator.
     * Only the replacement session (when CONDUCTED) counts.
     * See DOMAIN_RULES.md Rule S-005.
     */
    RESCHEDULED
}
