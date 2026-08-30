package com.amcs.domain.attendance;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a single class or laboratory session occurrence.
 *
 * <h3>Planned Units vs. Conducted Units</h3>
 * <ul>
 *   <li><b>{@code plannedUnits}</b>: The number of attendance units originally scheduled
 *       for this session in the timetable (must be $\ge 1$).</li>
 *   <li><b>{@code conductedUnits}</b>: The number of units actually conducted when held.
 *       <ul>
 *         <li>For {@link SessionStatus#CONDUCTED} sessions: must be $\ge 1$ (typically equals plannedUnits,
 *             unless institutionally adjusted).</li>
 *         <li>For {@link SessionStatus#CANCELLED}, {@link SessionStatus#SCHEDULED}, or
 *             {@link SessionStatus#RESCHEDULED} sessions: strictly $0$. A cancelled 3-unit lab
 *             contributes exactly 0 conducted units to attendance.</li>
 *       </ul>
 *   </li>
 *   <li><b>{@code isCountable()}</b>: The single source of truth for denominator inclusion.
 *       Evaluates {@code status == SessionStatus.CONDUCTED && conductedUnits > 0}.</li>
 * </ul>
 */
public record Session(
    UUID id,
    UUID subjectId,
    UUID sectionId,
    UUID conductedByFacultyId,
    LocalDate sessionDate,
    SessionType sessionType,
    int plannedUnits,
    int conductedUnits,
    SessionStatus status,
    Optional<UUID> labGroupId,
    Optional<UUID> replacedBySessionId
) {
    public Session {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(subjectId, "subjectId must not be null");
        Objects.requireNonNull(sectionId, "sectionId must not be null");
        Objects.requireNonNull(conductedByFacultyId, "conductedByFacultyId must not be null");
        Objects.requireNonNull(sessionDate, "sessionDate must not be null");
        Objects.requireNonNull(sessionType, "sessionType must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(labGroupId, "labGroupId must not be null");
        Objects.requireNonNull(replacedBySessionId, "replacedBySessionId must not be null");

        if (plannedUnits < 1) {
            throw new IllegalArgumentException(
                "plannedUnits must be >= 1. A session must be scheduled for at least one unit. Got: "
                    + plannedUnits);
        }

        if (status == SessionStatus.CONDUCTED) {
            if (conductedUnits < 1) {
                throw new IllegalArgumentException(
                    "conductedUnits must be >= 1 for a CONDUCTED session. Got: " + conductedUnits);
            }
        } else {
            if (conductedUnits != 0) {
                throw new IllegalArgumentException(
                    "conductedUnits must be 0 for non-CONDUCTED session (status: " + status
                        + "), got: " + conductedUnits);
            }
        }
    }

    /**
     * Backward-compatible convenience constructor.
     * Automatically sets {@code conductedUnits = 0} if status is not CONDUCTED.
     */
    public Session(
        UUID id,
        UUID subjectId,
        UUID sectionId,
        UUID conductedByFacultyId,
        LocalDate sessionDate,
        SessionType sessionType,
        int units,
        SessionStatus status,
        Optional<UUID> labGroupId,
        Optional<UUID> replacedBySessionId
    ) {
        this(id, subjectId, sectionId, conductedByFacultyId, sessionDate, sessionType,
            units,
            status == SessionStatus.CONDUCTED ? units : 0,
            status, labGroupId, replacedBySessionId);
    }

    /**
     * Returns {@code true} if this session should contribute to the attendance denominator.
     * Only {@link SessionStatus#CONDUCTED} sessions with {@code conductedUnits > 0} count.
     */
    public boolean isCountable() {
        return status == SessionStatus.CONDUCTED && conductedUnits > 0;
    }
}
