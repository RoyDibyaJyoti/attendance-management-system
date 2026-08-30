package com.amcs.domain.attendance;

/**
 * Represents the status of a student's attendance in a single session.
 *
 * <p><strong>CRITICAL DESIGN PRINCIPLE:</strong> The contribution of each status to the
 * attendance numerator is determined SOLELY by the
 * {@link com.amcs.domain.policy.AttendancePolicy#getContribution(AttendanceStatus)}
 * method. Do NOT hard-code any assumption that DUTY_LEAVE = present, or that
 * MEDICAL_LEAVE = absent. The mapping is entirely policy-driven.
 *
 * <p>Additional statuses (HALF_DAY, LATE, etc.) can be added to this enum and to
 * the policy's statusContributions map without any change to the calculation engine.
 * This design is intentional.
 *
 * <p>REQUIRES INSTITUTIONAL CONFIRMATION (RIC-AS-002):
 * Which of these statuses does the institution actually use?
 * What is the contribution fraction of each?
 */
public enum AttendanceStatus {

    /** Student was present for the session. */
    PRESENT,

    /** Student was absent from the session. */
    ABSENT,

    /**
     * Student was absent due to official institutional duty
     * (sports, cultural events, inter-college competitions, etc.).
     * Contribution REQUIRES INSTITUTIONAL CONFIRMATION.
     */
    DUTY_LEAVE,

    /**
     * Student was absent due to a documented medical reason.
     * Contribution REQUIRES INSTITUTIONAL CONFIRMATION.
     */
    MEDICAL_LEAVE,

    /**
     * Student was absent due to a faculty-sanctioned on-duty assignment.
     * Contribution REQUIRES INSTITUTIONAL CONFIRMATION.
     */
    ON_DUTY
}
