package com.amcs.application.dto.report;

import java.math.BigDecimal;
import java.util.UUID;

public record OverallAttendanceReportRow(
    UUID studentId,
    String registrationNumber,
    String studentName,
    String sectionName,
    int subjectsCount,
    int totalConductedUnits,
    int totalAttendedUnits,
    BigDecimal overallPercentage,
    BigDecimal thresholdPercentage,
    String overallStatus
) {}
