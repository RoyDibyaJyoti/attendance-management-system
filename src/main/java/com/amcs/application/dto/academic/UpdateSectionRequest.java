package com.amcs.application.dto.academic;

import jakarta.validation.constraints.Size;

public record UpdateSectionRequest(
    @Size(min = 1, max = 50, message = "Name must be between 1 and 50 characters")
    String name
) {}
