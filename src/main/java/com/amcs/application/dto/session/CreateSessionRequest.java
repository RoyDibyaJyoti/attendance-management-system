package com.amcs.application.dto.session;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.UUID;

public record CreateSessionRequest(
    @NotNull(message = "Subject ID must not be null")
    UUID subjectId,

    @NotNull(message = "Section ID must not be null")
    UUID sectionId,

    @NotNull(message = "Faculty ID must not be null")
    UUID conductedByFacultyId,

    @NotNull(message = "Academic period ID must not be null")
    UUID academicPeriodId,

    @NotNull(message = "Session date must not be null")
    LocalDate sessionDate,

    @NotBlank(message = "Session type must not be blank")
    @Pattern(regexp = "THEORY|LAB", message = "Session type must be THEORY or LAB")
    String sessionType,

    @NotNull(message = "Planned units must not be null")
    @Min(value = 1, message = "Planned units must be at least 1")
    Integer plannedUnits,

    UUID labGroupId
) {}
