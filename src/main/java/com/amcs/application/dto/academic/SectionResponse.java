package com.amcs.application.dto.academic;

import java.util.UUID;

public record SectionResponse(
    UUID id,
    String name,
    UUID departmentId,
    UUID academicPeriodId,
    boolean isActive
) {}
