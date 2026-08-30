package com.amcs.application.dto.academic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateSectionRequest(
    @NotBlank(message = "Section name must not be blank")
    @Size(min = 1, max = 50, message = "Section name must be between 1 and 50 characters")
    String name,

    @NotNull(message = "Department ID must not be null")
    UUID departmentId,

    @NotNull(message = "Academic period ID must not be null")
    UUID academicPeriodId
) {}
