package com.amcs.application.dto.policy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OverallAttendancePolicyResponse(
    UUID id,
    String name,
    int version,
    String aggregationStrategy,
    BigDecimal minimumThresholdPercentage,
    Instant effectiveFrom,
    Instant effectiveTo,
    boolean active
) {}
