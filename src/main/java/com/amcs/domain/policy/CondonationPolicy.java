package com.amcs.domain.policy;

import com.amcs.domain.calculation.result.SubjectAttendanceResult;

/**
 * PLACEHOLDER — Condonation policy abstraction.
 *
 * <p><strong>ARCHITECTURAL BOUNDARY MANDATE:</strong>
 * Condonation MUST NEVER silently modify ordinary attendance calculations or alter
 * the computed percentage in {@link SubjectAttendanceResult}.
 * Condonation is strictly a post-calculation layer that evaluates an already-computed
 * shortage against institutional condonation rules:
 *
 * <pre>
 *   Raw Attendance
 *         ↓
 *   Normal Attendance Result (SubjectAttendanceResult)
 *         ↓
 *   Shortage Analysis (ShortageAnalysis)
 *         ↓
 *   Condonation Policy (CondonationPolicy)
 *         ↓
 *   Final Eligibility / Adjusted Result
 * </pre>
 *
 * <p><strong>IMPLEMENTATION BLOCKED: REQUIRES INSTITUTIONAL CONFIRMATION.</strong>
 * Condonation rules remain unconfirmed by institutional stakeholders:
 * <ul>
 *   <li>RIC-SC-003: Does the institution allow condonation?</li>
 *   <li>RIC-SC-004: What conditions trigger condonation (medical leave, duty leave, narrow attendance band)?</li>
 *   <li>RIC-SC-005: How is condonation applied (percentage points, units, absence forgiveness)?</li>
 *   <li>RIC-SC-006: What is the maximum condonation allowance and minimum hard floor?</li>
 * </ul>
 */
public record CondonationPolicy(String ruleDescription) {

    /**
     * Post-calculation adjustment hook.
     *
     * @throws UnsupportedOperationException always — condonation rules REQUIRE
     *         INSTITUTIONAL CONFIRMATION before implementation.
     */
    public void evaluateAdjustment(SubjectAttendanceResult ordinaryResult) {
        throw new UnsupportedOperationException(
            "Condonation calculation is not implemented. "
                + "Institutional confirmation of rules RIC-SC-003 through RIC-SC-006 "
                + "is required before this post-calculation layer can be implemented. "
                + "See DOMAIN_RULES.md Section 8.");
    }
}
