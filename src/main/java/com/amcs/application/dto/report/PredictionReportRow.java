package com.amcs.application.dto.report;

import java.math.BigDecimal;
import java.util.UUID;

public record PredictionReportRow(
    UUID studentId,
    String registrationNumber,
    String studentName,
    String subjectCode,
    String subjectName,
    int currentConducted,
    int currentAttended,
    BigDecimal currentPercentage,
    BigDecimal targetThresholdPercentage,
    int projectedFutureSessions,
    int requiredFutureSessions,
    String feasibilityStatus
) {}
