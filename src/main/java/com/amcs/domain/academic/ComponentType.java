package com.amcs.domain.academic;

/**
 * Identifies which component of a subject a session or policy applies to.
 * Relevant only for {@link CourseType#THEORY_INTEGRATED_LABORATORY} subjects.
 */
public enum ComponentType {
    THEORY,
    LAB
}
