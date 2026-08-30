package com.amcs.domain.enrollment;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Records a student's enrollment in a section, with temporal validity.
 *
 * <h3>Enrollment Window (CONFIRMED — DI-003)</h3>
 * <p>Only sessions conducted on dates when the student's enrollment is active
 * are counted in their attendance denominator. A student who joins mid-semester
 * must NOT be penalized for sessions conducted before their enrollment date.
 * A student who transfers out must NOT have post-transfer sessions counted.
 *
 * <h3>Lab Group Membership</h3>
 * <p>If the student belongs to a lab sub-group (e.g., Group A), they should only
 * participate in lab sessions assigned to that group. Lab sessions assigned to
 * another group (Group B) must not be counted in their denominator.
 * See DOMAIN_RULES.md Rule L-004 (REQUIRES INSTITUTIONAL CONFIRMATION for
 * exact group tracking model).
 *
 * <h3>Transfer / Withdrawal</h3>
 * <p>A student may have multiple enrollment records for different sections in the
 * same academic period (before and after a transfer). The calculation engine must
 * use the appropriate enrollment record's window for each session.
 */
public record Enrollment(
    UUID studentId,
    UUID sectionId,
    LocalDate enrollmentStart,
    Optional<LocalDate> enrollmentEnd,
    Optional<UUID> labGroupId
) {
    public Enrollment {
        Objects.requireNonNull(studentId, "studentId must not be null");
        Objects.requireNonNull(sectionId, "sectionId must not be null");
        Objects.requireNonNull(enrollmentStart, "enrollmentStart must not be null");
        Objects.requireNonNull(enrollmentEnd, "enrollmentEnd must not be null");
        Objects.requireNonNull(labGroupId, "labGroupId must not be null");
        enrollmentEnd.ifPresent(end -> {
            if (end.isBefore(enrollmentStart)) {
                throw new IllegalArgumentException(
                    "enrollmentEnd [%s] must not be before enrollmentStart [%s]"
                        .formatted(end, enrollmentStart));
            }
        });
    }

    /**
     * Returns {@code true} if this enrollment is active on the given date.
     *
     * <p>Active means: date >= enrollmentStart AND (no end date OR date <= enrollmentEnd).
     */
    public boolean isActiveOn(LocalDate date) {
        Objects.requireNonNull(date, "date must not be null");
        if (date.isBefore(enrollmentStart)) return false;
        return enrollmentEnd.map(end -> !date.isAfter(end)).orElse(true);
    }

    /**
     * Returns {@code true} if this student is eligible to participate in a session
     * belonging to the given lab group.
     *
     * <p>Rules:
     * <ol>
     *   <li>If the session has no lab group ({@code sessionLabGroupId} is empty):
     *       the session applies to all students — always {@code true}.</li>
     *   <li>If the session IS group-specific but the student has no lab group assignment:
     *       {@code false} — the student should not participate in group-split sessions.</li>
     *   <li>If both have lab groups: {@code true} only if they match.</li>
     * </ol>
     */
    public boolean isEligibleForLabGroup(Optional<UUID> sessionLabGroupId) {
        Objects.requireNonNull(sessionLabGroupId, "sessionLabGroupId must not be null");
        // Session is not group-specific: applies to everyone
        if (sessionLabGroupId.isEmpty()) return true;
        // Session is group-specific; student must belong to that exact group
        return labGroupId.equals(sessionLabGroupId);
    }
}
