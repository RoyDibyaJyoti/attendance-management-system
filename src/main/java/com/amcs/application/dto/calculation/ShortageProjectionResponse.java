package com.amcs.application.dto.calculation;

import java.math.BigDecimal;
import java.util.UUID;

public record ShortageProjectionResponse(
    UUID studentId,
    UUID subjectId,
    BigDecimal currentPercentage,
    BigDecimal thresholdPercentage,
    BigDecimal shortageUnits,
    BigDecimal surplusUnits,
    int minimumAdditionalUnitsRequired,
    int maximumAllowableAbsenceUnits,
    int projectedRemainingUnits,
    boolean isPossibleToRecover
) {}
