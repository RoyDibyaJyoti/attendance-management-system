package com.amcs.application.dto.enrollment;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record EnrollStudentRequest(
    @NotNull(message = "Student ID must not be null")
    UUID studentId,

    @NotNull(message = "Section ID must not be null")
    UUID sectionId,

    @NotNull(message = "Enrollment start date must not be null")
    LocalDate enrollmentStart,

    LocalDate enrollmentEnd
) {}
