package com.amcs.application.service.importer.payload;

import com.amcs.domain.attendance.SessionType;

import java.time.LocalDate;
import java.util.UUID;

public record StagedSessionPayload(
    UUID academicPeriodId,
    UUID subjectId,
    UUID sectionId,
    UUID facultyId,
    LocalDate sessionDate,
    SessionType sessionType,
    int plannedUnits,
    UUID labGroupId
) {}
