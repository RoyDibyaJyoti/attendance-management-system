package com.amcs.domain.policy;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/**
 * Configures how the theory and lab components of a
 * {@link com.amcs.domain.academic.CourseType#THEORY_INTEGRATED_LABORATORY}
 * subject are combined into a final subject attendance result.
 *
 * <p>The strategy must be set according to institutional rules.
 * See {@link IntegratedCompositeStrategy} for descriptions of each option.
 * REQUIRES INSTITUTIONAL CONFIRMATION (RIC-TL-002).
 *
 * <p>Use the static factory methods to create instances in a validated state.
 */
public record IntegratedSubjectPolicy(
    IntegratedCompositeStrategy strategy,
    Optional<AttendancePolicy> combinedPolicy,
    Optional<BigDecimal> theoryWeight,
    Optional<BigDecimal> labWeight,
    Optional<BigDecimal> compositeThresholdPercentage
) {
    public IntegratedSubjectPolicy {
        Objects.requireNonNull(strategy, "strategy must not be null");
        Objects.requireNonNull(combinedPolicy, "combinedPolicy must not be null");
        Objects.requireNonNull(theoryWeight, "theoryWeight must not be null");
        Objects.requireNonNull(labWeight, "labWeight must not be null");
        Objects.requireNonNull(compositeThresholdPercentage, "compositeThresholdPercentage must not be null");

        switch (strategy) {
            case SEPARATE_THRESHOLDS -> {
                // No extra fields needed; each component uses its own policy's threshold
            }
            case COMBINED_ALL_SESSIONS -> {
                if (combinedPolicy.isEmpty()) {
                    throw new IllegalArgumentException(
                        "combinedPolicy must be present for COMBINED_ALL_SESSIONS strategy");
                }
            }
            case WEIGHTED_AVERAGE -> {
                if (theoryWeight.isEmpty() || labWeight.isEmpty()) {
                    throw new IllegalArgumentException(
                        "theoryWeight and labWeight must be present for WEIGHTED_AVERAGE strategy");
                }
                if (compositeThresholdPercentage.isEmpty()) {
                    throw new IllegalArgumentException(
                        "compositeThresholdPercentage must be present for WEIGHTED_AVERAGE strategy");
                }
                BigDecimal tw = theoryWeight.get();
                BigDecimal lw = labWeight.get();
                BigDecimal sum = tw.add(lw);
                // Allow a tiny tolerance for BigDecimal representation differences
                if (sum.compareTo(BigDecimal.ONE) != 0) {
                    throw new IllegalArgumentException(
                        "theoryWeight + labWeight must equal exactly 1.0, got: " + sum);
                }
                BigDecimal threshold = compositeThresholdPercentage.get();
                if (threshold.compareTo(BigDecimal.ZERO) < 0
                    || threshold.compareTo(new BigDecimal("100")) > 0) {
                    throw new IllegalArgumentException(
                        "compositeThresholdPercentage must be in [0, 100], got: " + threshold);
                }
            }
        }
    }

    // =========================================================================
    // Static factory methods
    // =========================================================================

    /** Creates a policy using SEPARATE_THRESHOLDS strategy. */
    public static IntegratedSubjectPolicy separateThresholds() {
        return new IntegratedSubjectPolicy(
            IntegratedCompositeStrategy.SEPARATE_THRESHOLDS,
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    /** Creates a policy using COMBINED_ALL_SESSIONS strategy with the given combined policy. */
    public static IntegratedSubjectPolicy combinedAllSessions(AttendancePolicy combinedPolicy) {
        Objects.requireNonNull(combinedPolicy, "combinedPolicy must not be null");
        return new IntegratedSubjectPolicy(
            IntegratedCompositeStrategy.COMBINED_ALL_SESSIONS,
            Optional.of(combinedPolicy), Optional.empty(), Optional.empty(), Optional.empty());
    }

    /**
     * Creates a policy using WEIGHTED_AVERAGE strategy.
     *
     * @param theoryWeight            weight for theory component (must be in [0,1])
     * @param labWeight               weight for lab component (must be in [0,1])
     * @param compositeThreshold      composite threshold percentage (in [0,100])
     * @throws IllegalArgumentException if weights do not sum to exactly 1.0
     */
    public static IntegratedSubjectPolicy weightedAverage(
        BigDecimal theoryWeight, BigDecimal labWeight, BigDecimal compositeThreshold) {
        Objects.requireNonNull(theoryWeight, "theoryWeight must not be null");
        Objects.requireNonNull(labWeight, "labWeight must not be null");
        Objects.requireNonNull(compositeThreshold, "compositeThreshold must not be null");
        return new IntegratedSubjectPolicy(
            IntegratedCompositeStrategy.WEIGHTED_AVERAGE,
            Optional.empty(),
            Optional.of(theoryWeight),
            Optional.of(labWeight),
            Optional.of(compositeThreshold));
    }
}
