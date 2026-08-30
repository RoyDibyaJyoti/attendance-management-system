package com.amcs.application.dto.labgroup;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record AssignLabGroupRequest(
    @NotNull(message = "Student ID must not be null")
    UUID studentId,

    @NotNull(message = "Lab group ID must not be null")
    UUID labGroupId,

    @NotNull(message = "Effective start date must not be null")
    LocalDate effectiveStart,

    LocalDate effectiveEnd
) {}
