package com.amcs.application.dto.policy;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record CreateAttendancePolicyRequest(
    @NotBlank(message = "Policy name must not be blank")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    String name,

    @NotNull(message = "Minimum threshold percentage must not be null")
    @DecimalMin(value = "0.00", message = "Threshold must be at least 0.00")
    @DecimalMax(value = "100.00", message = "Threshold cannot exceed 100.00")
    BigDecimal minimumThresholdPercentage,

    @NotEmpty(message = "Status contributions map must not be empty")
    Map<String, BigDecimal> statusContributions,

    @NotBlank(message = "Missing record strategy must not be blank")
    @Pattern(regexp = "TREAT_AS_ABSENT|EXCLUDE_FROM_CALCULATION|MARK_AS_INCOMPLETE",
             message = "Strategy must be TREAT_AS_ABSENT, EXCLUDE_FROM_CALCULATION, or MARK_AS_INCOMPLETE")
    String missingRecordStrategy,

    @NotNull(message = "Effective from must not be null")
    Instant effectiveFrom,

    Instant effectiveTo
) {}
