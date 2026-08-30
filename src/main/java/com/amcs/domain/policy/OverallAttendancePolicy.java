package com.amcs.domain.policy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Defines how subject-level attendance results are aggregated into an overall
 * attendance figure for a student in a given period.
 *
 * <p>CRITICAL: The correct {@link OverallAggregationStrategy} and the
 * {@code minimumThresholdPercentage} for overall attendance MUST be confirmed
 * by the institution before this policy is used for official purposes.
 * See DOMAIN_RULES.md Rules OA-002 and OA-003.
 *
 * <p>The overall threshold may differ from subject-level thresholds.
 * It must NOT be assumed to be the same value.
 */
public record OverallAttendancePolicy(
    UUID id,
    String name,
    int version,
    OverallAggregationStrategy aggregationStrategy,
    BigDecimal minimumThresholdPercentage,
    Instant effectiveFrom,
    Optional<Instant> effectiveTo
) {
    public OverallAttendancePolicy {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(aggregationStrategy, "aggregationStrategy must not be null");
        Objects.requireNonNull(minimumThresholdPercentage, "minimumThresholdPercentage must not be null");
        Objects.requireNonNull(effectiveFrom, "effectiveFrom must not be null");
        Objects.requireNonNull(effectiveTo, "effectiveTo must not be null");
        if (name.isBlank()) throw new IllegalArgumentException("name must not be blank");
        if (version < 1) throw new IllegalArgumentException("version must be >= 1, got: " + version);
        if (minimumThresholdPercentage.compareTo(BigDecimal.ZERO) < 0
            || minimumThresholdPercentage.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException(
                "minimumThresholdPercentage must be in [0, 100], got: " + minimumThresholdPercentage);
        }
    }

    /** Convenience factory for tests and configuration. */
    public static OverallAttendancePolicy of(
        UUID id, String name, OverallAggregationStrategy strategy, BigDecimal threshold) {
        return new OverallAttendancePolicy(
            id, name, 1, strategy, threshold, Instant.now(), Optional.empty());
    }
}
