package com.amcs.application.dto.enrollment;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record TransferStudentRequest(
    @NotNull(message = "Student ID must not be null")
    UUID studentId,

    @NotNull(message = "Current section ID must not be null")
    UUID fromSectionId,

    @NotNull(message = "Target section ID must not be null")
    UUID toSectionId,

    @NotNull(message = "Transfer effective date must not be null")
    LocalDate transferDate
) {}
