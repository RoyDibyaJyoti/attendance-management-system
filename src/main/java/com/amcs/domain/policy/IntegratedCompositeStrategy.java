package com.amcs.domain.policy;

/**
 * Defines how the theory and lab components of a
 * {@link com.amcs.domain.academic.CourseType#THEORY_INTEGRATED_LABORATORY} subject
 * are combined to produce a final subject-level attendance result.
 *
 * <p><strong>REQUIRES INSTITUTIONAL CONFIRMATION (RIC-TL-002).</strong>
 * All three strategies are modelled because different institutions use each.
 * Do NOT default to any strategy without institutional confirmation.
 */
public enum IntegratedCompositeStrategy {

    /**
     * Theory and lab attendance are evaluated independently against their own thresholds.
     * A student is in shortage if they fail EITHER component.
     * Both results are reported separately with no composite percentage.
     *
     * <p>Example: Theory threshold 75%, Lab threshold 80%.
     * Student with theory=78%, lab=75% → lab shortage despite adequate theory.
     */
    SEPARATE_THRESHOLDS,

    /**
     * All sessions (theory and lab) are pooled under a single combined policy.
     * Session type is used for no purpose other than unit counting
     * (via {@link com.amcs.domain.attendance.Session#conductedUnits()}).
     * One percentage and one threshold produce the result.
     *
     * <p>The combined policy is specified in {@link IntegratedSubjectPolicy#combinedPolicy()}.
     */
    COMBINED_ALL_SESSIONS,

    /**
     * Composite percentage = (theory% × theoryWeight) + (lab% × labWeight).
     * Compared against a single composite threshold.
     * Weights must sum to exactly 1.0.
     *
     * <p>Example: theory%=80, lab%=70, theoryWeight=0.6, labWeight=0.4
     * Composite = 0.6×80 + 0.4×70 = 48 + 28 = 76%
     */
    WEIGHTED_AVERAGE
}
