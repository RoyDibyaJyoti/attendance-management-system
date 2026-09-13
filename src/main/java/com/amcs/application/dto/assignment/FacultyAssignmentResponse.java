package com.amcs.application.dto.assignment;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FacultyAssignmentResponse(
    UUID id,
    UUID facultyId,
    UUID subjectId,
    UUID sectionId,
    UUID academicPeriodId,
    LocalDate assignmentStart,
    LocalDate assignmentEnd,
    String status,
    Instant createdAt
) {}
