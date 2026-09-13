package com.amcs.application.port.out.security;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Application-facing representation of a faculty teaching assignment.
 */
public record FacultyAssignment(
    UUID id,
    UUID facultyId,
    UUID subjectId,
    UUID sectionId,
    UUID academicPeriodId,
    LocalDate assignmentStart,
    LocalDate assignmentEnd,
    String status,
    Instant createdAt
) {
    public FacultyAssignment {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(facultyId, "facultyId must not be null");
        Objects.requireNonNull(subjectId, "subjectId must not be null");
        Objects.requireNonNull(sectionId, "sectionId must not be null");
        Objects.requireNonNull(academicPeriodId, "academicPeriodId must not be null");
        Objects.requireNonNull(assignmentStart, "assignmentStart must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        
        if (assignmentEnd != null && assignmentEnd.isBefore(assignmentStart)) {
            throw new IllegalArgumentException("assignmentEnd cannot be before assignmentStart");
        }
    }
}
