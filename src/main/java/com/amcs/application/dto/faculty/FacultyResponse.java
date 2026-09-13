package com.amcs.application.dto.faculty;

import java.util.UUID;

public record FacultyResponse(
    UUID id,
    String employeeId,
    String name,
    String email,
    UUID departmentId,
    boolean isActive
) {}
