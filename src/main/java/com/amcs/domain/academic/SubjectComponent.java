package com.amcs.domain.academic;

import com.amcs.domain.policy.AttendancePolicy;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents one component (THEORY or LAB) of a
 * {@link CourseType#THEORY_INTEGRATED_LABORATORY} subject.
 *
 * <p>Each component has its own {@link AttendancePolicy}, which means theory and lab
 * sessions within the same subject can have different:
 * <ul>
 *   <li>Thresholds</li>
 *   <li>Status contributions (e.g., DUTY_LEAVE may count differently for labs)</li>
 *   <li>Unit weights</li>
 * </ul>
 *
 * <p>This is the primary mechanism by which the system avoids mixing theory and lab
 * calculation semantics. See ARCHITECTURE_NOTES.md ADR-001 and DOMAIN_RULES.md Section 6.
 */
public record SubjectComponent(
    UUID subjectId,
    ComponentType componentType,
    AttendancePolicy policy
) {
    public SubjectComponent {
        Objects.requireNonNull(subjectId, "subjectId must not be null");
        Objects.requireNonNull(componentType, "componentType must not be null");
        Objects.requireNonNull(policy, "policy must not be null");
    }
}
