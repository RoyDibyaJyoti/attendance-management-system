package com.amcs.application.dto.student;

import java.time.Instant;
import java.util.UUID;

public record StudentResponse(
    UUID id,
    String registrationNumber,
    String name,
    String email,
    UUID departmentId,
    Instant createdAt,
    boolean isActive
) {}
