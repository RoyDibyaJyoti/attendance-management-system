package com.amcs.application.dto.academic;

import java.util.UUID;

public record DepartmentResponse(
    UUID id,
    String code,
    String name
) {}
