package com.amcs.application.dto.calculation;

import java.math.BigDecimal;
import java.util.UUID;

public record SubjectAttendanceSummaryResponse(
    UUID studentId,
    UUID subjectId,
    String subjectCode,
    String subjectName,
    String policyName,
    int policyVersion,
    BigDecimal thresholdPercentage,
    BigDecimal conductedUnits,
    BigDecimal attendedUnits,
    BigDecimal attendancePercentage,
    String classification,
    boolean isAdequate,
    boolean isShortage,
    BigDecimal shortageUnits,
    BigDecimal surplusUnits,
    int missingRecordCount,
    boolean isIncomplete
) {}
