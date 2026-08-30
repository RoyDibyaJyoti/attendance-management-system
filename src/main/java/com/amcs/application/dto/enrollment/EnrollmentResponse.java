package com.amcs.application.dto.enrollment;

import java.time.LocalDate;
import java.util.UUID;

public record EnrollmentResponse(
    UUID id,
    UUID studentId,
    UUID sectionId,
    LocalDate enrollmentStart,
    LocalDate enrollmentEnd,
    String status
) {}
