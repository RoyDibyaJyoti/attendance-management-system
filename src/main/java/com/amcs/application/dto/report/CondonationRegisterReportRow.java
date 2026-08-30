package com.amcs.application.dto.report;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CondonationRegisterReportRow(
    UUID recordId,
    LocalDate sessionDate,
    String registrationNumber,
    String studentName,
    String subjectCode,
    String subjectName,
    String sectionName,
    String conductedByFacultyName,
    String leaveType,
    Instant recordedAt
) {}
