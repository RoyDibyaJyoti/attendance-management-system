package com.amcs.application.dto.report;

import java.math.BigDecimal;
import java.util.UUID;

public record FacultyComplianceReportRow(
    UUID facultyId,
    String employeeId,
    String facultyName,
    String departmentName,
    String subjectCode,
    String subjectName,
    String sectionName,
    int totalScheduledSessions,
    int conductedSessions,
    int unconductedSessions,
    BigDecimal compliancePercentage
) {}
