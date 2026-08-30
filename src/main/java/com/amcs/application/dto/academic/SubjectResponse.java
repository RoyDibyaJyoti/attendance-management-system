package com.amcs.application.dto.academic;

import java.util.UUID;

public record SubjectResponse(
    UUID id,
    String code,
    String name,
    String courseType,
    int creditHours
) {}
