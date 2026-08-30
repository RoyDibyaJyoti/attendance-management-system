package com.amcs.application.dto.session;

import java.time.LocalDate;
import java.util.UUID;

public record SessionResponse(
    UUID id,
    UUID subjectId,
    UUID sectionId,
    UUID conductedByFacultyId,
    LocalDate sessionDate,
    String sessionType,
    int plannedUnits,
    int conductedUnits,
    String status,
    UUID labGroupId,
    UUID replacedBySessionId,
    Integer version
) {}
