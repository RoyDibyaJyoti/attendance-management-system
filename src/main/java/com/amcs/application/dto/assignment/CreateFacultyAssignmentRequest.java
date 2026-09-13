package com.amcs.application.dto.assignment;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record CreateFacultyAssignmentRequest(
    @NotNull(message = "Faculty ID is required")
    UUID facultyId,
    
    @NotNull(message = "Subject ID is required")
    UUID subjectId,
    
    @NotNull(message = "Section ID is required")
    UUID sectionId,
    
    @NotNull(message = "Academic Period ID is required")
    UUID academicPeriodId,
    
    @NotNull(message = "Assignment start date is required")
    LocalDate assignmentStart,
    
    LocalDate assignmentEnd
) {}
