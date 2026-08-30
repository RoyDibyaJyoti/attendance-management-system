package com.amcs.domain.academic;

/**
 * Classifies the type of a subject/course.
 *
 * <p>This determines which session streams exist and which calculation semantics apply.
 * Theory and laboratory courses intentionally use different calculation paths.
 *
 * <p>REQUIRES INSTITUTIONAL CONFIRMATION: Are there additional course types
 * (tutorial, seminar, fieldwork, project)? Each would need its own policy mapping.
 * See DOMAIN_RULES.md FR-005.
 */
public enum CourseType {

    /**
     * Pure theory course. Only {@link com.amcs.domain.attendance.SessionType#THEORY}
     * sessions exist for this subject. The theory attendance IS the subject attendance.
     */
    THEORY,

    /**
     * Pure laboratory course. Only {@link com.amcs.domain.attendance.SessionType#LAB}
     * sessions exist. The lab attendance IS the subject attendance.
     *
     * <p>Unit counting for lab sessions REQUIRES INSTITUTIONAL CONFIRMATION (RIC-S-003).
     * Do NOT assume 1 session = 1 unit.
     */
    LABORATORY,

    /**
     * Subject containing both theory and laboratory components.
     * Has two distinct session streams, potentially governed by separate policies.
     *
     * <p>How the two components are combined into a single subject result
     * REQUIRES INSTITUTIONAL CONFIRMATION (RIC-TL-002).
     * Refer to {@link com.amcs.domain.policy.IntegratedSubjectPolicy}.
     */
    THEORY_INTEGRATED_LABORATORY
}
