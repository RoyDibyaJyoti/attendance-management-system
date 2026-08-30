package com.amcs.domain.policy;

import com.amcs.domain.attendance.AttendanceStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Defines how attendance is calculated for a subject (or a component of an integrated subject).
 *
 * <h3>Design Principles</h3>
 * <ul>
 *   <li><b>Immutable:</b> A policy is never mutated. A "change" creates a new version.</li>
 *   <li><b>Versioned:</b> Every calculation result records the policyId + version used.
 *       Historical results remain reproducible even after policy changes.</li>
 *   <li><b>No default thresholds:</b> {@code minimumThresholdPercentage} MUST be
 *       explicitly configured. There is no system-level default threshold.
 *       Different departments or subjects may have different thresholds.</li>
 *   <li><b>Status contributions are a map, not hard-coded logic:</b> Adding a new
 *       AttendanceStatus (e.g., HALF_DAY) only requires adding it to this map —
 *       the calculation engine needs no changes.</li>
 *   <li><b>Configurable Missing Record Handling:</b> {@link MissingRecordStrategy} explicitly
 *       declares how missing attendance records are evaluated (TREAT_AS_ABSENT,
 *       EXCLUDE_FROM_CALCULATION, MARK_AS_INCOMPLETE).</li>
 *   <li><b>Safe default for unknown statuses:</b> {@link #getContribution(AttendanceStatus)}
 *       returns ZERO for any status not in the map. This is the conservative choice:
 *       an unrecognized status does not accidentally inflate attendance.</li>
 * </ul>
 */
public record AttendancePolicy(
    UUID id,
    String name,
    int version,
    BigDecimal minimumThresholdPercentage,
    Map<AttendanceStatus, BigDecimal> statusContributions,
    MissingRecordStrategy missingRecordStrategy,
    Optional<CondonationPolicy> condonationPolicy,
    Instant effectiveFrom,
    Optional<Instant> effectiveTo
) {
    /** Scale used for the threshold fraction (0-1 form) during intermediate computation. */
    public static final int THRESHOLD_FRACTION_SCALE = 10;

    public AttendancePolicy {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(minimumThresholdPercentage, "minimumThresholdPercentage must not be null");
        Objects.requireNonNull(statusContributions, "statusContributions must not be null");
        Objects.requireNonNull(missingRecordStrategy, "missingRecordStrategy must not be null");
        Objects.requireNonNull(condonationPolicy, "condonationPolicy must not be null");
        Objects.requireNonNull(effectiveFrom, "effectiveFrom must not be null");
        Objects.requireNonNull(effectiveTo, "effectiveTo must not be null");

        if (name.isBlank()) throw new IllegalArgumentException("name must not be blank");
        if (version < 1) throw new IllegalArgumentException("version must be >= 1, got: " + version);

        if (minimumThresholdPercentage.compareTo(BigDecimal.ZERO) < 0
            || minimumThresholdPercentage.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException(
                "minimumThresholdPercentage must be in [0, 100], got: " + minimumThresholdPercentage);
        }

        for (var entry : statusContributions.entrySet()) {
            BigDecimal contribution = entry.getValue();
            Objects.requireNonNull(contribution,
                "contribution for status [%s] must not be null".formatted(entry.getKey()));
            if (contribution.compareTo(BigDecimal.ZERO) < 0
                || contribution.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException(
                    "Contribution for status [%s] must be in [0, 1], got: %s"
                        .formatted(entry.getKey(), contribution));
            }
        }

        // Defensive copy: ensure the policy cannot be mutated externally
        statusContributions = Map.copyOf(statusContributions);
    }

    /**
     * Backward-compatible constructor defaulting to {@link MissingRecordStrategy#TREAT_AS_ABSENT}.
     */
    public AttendancePolicy(
        UUID id,
        String name,
        int version,
        BigDecimal minimumThresholdPercentage,
        Map<AttendanceStatus, BigDecimal> statusContributions,
        Optional<CondonationPolicy> condonationPolicy,
        Instant effectiveFrom,
        Optional<Instant> effectiveTo
    ) {
        this(id, name, version, minimumThresholdPercentage, statusContributions,
            MissingRecordStrategy.TREAT_AS_ABSENT, condonationPolicy, effectiveFrom, effectiveTo);
    }

    /**
     * Returns the attendance contribution fraction for the given status.
     *
     * <p>If the status is not found in the policy's map, returns {@link BigDecimal#ZERO}.
     */
    public BigDecimal getContribution(AttendanceStatus status) {
        Objects.requireNonNull(status, "status must not be null");
        return statusContributions.getOrDefault(status, BigDecimal.ZERO);
    }

    /**
     * Returns the threshold as a fraction in [0, 1] suitable for mathematical operations.
     */
    public BigDecimal thresholdFraction() {
        return minimumThresholdPercentage.divide(new BigDecimal("100"),
            THRESHOLD_FRACTION_SCALE, RoundingMode.HALF_UP);
    }

    // =========================================================================
    // Factory / Builder helpers — for test and service use
    // =========================================================================

    public static AttendancePolicy withDefaultContributions(
        UUID id, String name, BigDecimal thresholdPercentage) {
        return withDefaultContributions(id, name, thresholdPercentage, MissingRecordStrategy.TREAT_AS_ABSENT);
    }

    public static AttendancePolicy withDefaultContributions(
        UUID id, String name, BigDecimal thresholdPercentage, MissingRecordStrategy missingStrategy) {

        Map<AttendanceStatus, BigDecimal> contributions = new EnumMap<>(AttendanceStatus.class);
        contributions.put(AttendanceStatus.PRESENT, BigDecimal.ONE);
        contributions.put(AttendanceStatus.ABSENT, BigDecimal.ZERO);
        contributions.put(AttendanceStatus.DUTY_LEAVE, BigDecimal.ONE);
        contributions.put(AttendanceStatus.MEDICAL_LEAVE, BigDecimal.ONE);
        contributions.put(AttendanceStatus.ON_DUTY, BigDecimal.ONE);

        return new AttendancePolicy(
            id, name, 1, thresholdPercentage, contributions, missingStrategy,
            Optional.empty(), Instant.now(), Optional.empty());
    }

    public static AttendancePolicy withContributions(
        UUID id, String name, int version, BigDecimal thresholdPercentage,
        Map<AttendanceStatus, BigDecimal> contributions) {
        return withContributions(id, name, version, thresholdPercentage, contributions,
            MissingRecordStrategy.TREAT_AS_ABSENT);
    }

    public static AttendancePolicy withContributions(
        UUID id, String name, int version, BigDecimal thresholdPercentage,
        Map<AttendanceStatus, BigDecimal> contributions,
        MissingRecordStrategy missingStrategy) {

        return new AttendancePolicy(
            id, name, version, thresholdPercentage, contributions, missingStrategy,
            Optional.empty(), Instant.now(), Optional.empty());
    }
}
