package com.amcs.domain.attendance;

/**
 * Identifies whether a session is a theory class or a laboratory session.
 *
 * <p>For {@link com.amcs.domain.academic.CourseType#THEORY_INTEGRATED_LABORATORY} subjects,
 * sessions are split by this type so that different policies can be applied
 * to each component independently.
 */
public enum SessionType {
    THEORY,
    LAB
}
