package com.amcs.application.dto.policy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AttendancePolicyResponse(
    UUID id,
    String name,
    int version,
    BigDecimal minimumThresholdPercentage,
    Map<String, BigDecimal> statusContributions,
    String missingRecordStrategy,
    Instant effectiveFrom,
    Instant effectiveTo,
    boolean active
) {}
