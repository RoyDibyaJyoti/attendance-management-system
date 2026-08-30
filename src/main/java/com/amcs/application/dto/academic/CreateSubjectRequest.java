package com.amcs.application.dto.academic;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateSubjectRequest(
    @NotBlank(message = "Subject code must not be blank")
    @Size(min = 2, max = 20, message = "Subject code must be between 2 and 20 characters")
    String code,

    @NotBlank(message = "Subject name must not be blank")
    @Size(min = 2, max = 100, message = "Subject name must be between 2 and 100 characters")
    String name,

    @NotBlank(message = "Course type must not be blank")
    @Pattern(regexp = "THEORY|LABORATORY|THEORY_INTEGRATED_LABORATORY", message = "Course type must be THEORY, LABORATORY, or THEORY_INTEGRATED_LABORATORY")
    String courseType,

    @NotNull(message = "Credit hours must not be null")
    @Min(value = 0, message = "Credit hours must be non-negative")
    Integer creditHours,

    @NotNull(message = "Department ID must not be null")
    UUID departmentId
) {}
