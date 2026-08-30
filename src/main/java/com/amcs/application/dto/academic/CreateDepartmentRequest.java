package com.amcs.application.dto.academic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDepartmentRequest(
    @NotBlank(message = "Department code must not be blank")
    @Size(min = 2, max = 20, message = "Department code must be between 2 and 20 characters")
    String code,

    @NotBlank(message = "Department name must not be blank")
    @Size(min = 2, max = 100, message = "Department name must be between 2 and 100 characters")
    String name
) {}
