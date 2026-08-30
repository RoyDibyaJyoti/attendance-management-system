package com.amcs.application.dto.policy;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record CreateOverallAttendancePolicyRequest(
    @NotBlank(message = "Policy name must not be blank")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    String name,

    @NotBlank(message = "Aggregation strategy must not be blank")
    @Pattern(regexp = "ARITHMETIC_MEAN|WEIGHTED_BY_CREDITS|AGGREGATE_UNITS|AGGREGATE_HOURS",
             message = "Strategy must be ARITHMETIC_MEAN, WEIGHTED_BY_CREDITS, AGGREGATE_UNITS, or AGGREGATE_HOURS")
    String aggregationStrategy,

    @NotNull(message = "Minimum threshold percentage must not be null")
    @DecimalMin(value = "0.00", message = "Threshold must be at least 0.00")
    @DecimalMax(value = "100.00", message = "Threshold cannot exceed 100.00")
    BigDecimal minimumThresholdPercentage,

    @NotNull(message = "Effective from must not be null")
    Instant effectiveFrom,

    Instant effectiveTo
) {}
