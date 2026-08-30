package com.amcs.application.dto.report;

import java.math.BigDecimal;
import java.util.UUID;

public record SubjectAttendanceSummaryReportRow(
    UUID studentId,
    String registrationNumber,
    String studentName,
    String departmentName,
    int conductedUnits,
    int attendedUnits,
    BigDecimal percentage,
    String status
) {}
