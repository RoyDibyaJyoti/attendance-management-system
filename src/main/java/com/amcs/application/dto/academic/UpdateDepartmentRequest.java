package com.amcs.application.dto.academic;

import jakarta.validation.constraints.Size;

public record UpdateDepartmentRequest(
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    String name
) {}
