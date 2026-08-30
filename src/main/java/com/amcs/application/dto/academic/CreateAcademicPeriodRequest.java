package com.amcs.application.dto.academic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateAcademicPeriodRequest(
    @NotBlank(message = "Academic period name must not be blank")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    String name,

    @NotNull(message = "Start date must not be null")
    LocalDate startDate,

    @NotNull(message = "End date must not be null")
    LocalDate endDate
) {}
