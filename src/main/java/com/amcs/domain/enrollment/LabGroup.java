package com.amcs.domain.enrollment;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a laboratory sub-group within a section.
 *
 * <p>When a section is split for staggered lab schedules (e.g., Group A attends
 * Monday labs, Group B attends Wednesday labs), each sub-group is represented here.
 *
 * <p>Each {@link Enrollment} record may carry a reference to a LabGroup,
 * and each {@link com.amcs.domain.attendance.Session} for a group-specific lab
 * carries the corresponding LabGroup ID. The calculation engine uses these IDs
 * to gate which students are counted for which lab sessions.
 *
 * <p>REQUIRES INSTITUTIONAL CONFIRMATION (RIC-L-004): The exact model for
 * lab group tracking must be confirmed before finalizing persistence design.
 */
public record LabGroup(UUID id, String name, UUID sectionId) {
    public LabGroup {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(sectionId, "sectionId must not be null");
        if (name.isBlank()) throw new IllegalArgumentException("name must not be blank");
    }
}
