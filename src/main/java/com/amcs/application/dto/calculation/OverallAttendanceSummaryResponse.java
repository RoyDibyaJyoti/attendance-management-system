package com.amcs.application.dto.calculation;

import java.math.BigDecimal;
import java.util.UUID;

public record OverallAttendanceSummaryResponse(
    UUID studentId,
    String policyName,
    String aggregationStrategy,
    BigDecimal thresholdPercentage,
    BigDecimal overallPercentage,
    String classification,
    boolean isAdequate,
    boolean isShortage,
    int evaluatedCourseCount
) {}
