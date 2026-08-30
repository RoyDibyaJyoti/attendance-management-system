package com.amcs.domain.calculation.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Detailed shortage/surplus analysis for a student in a given subject.
 *
 * <p>Separate from {@link SubjectAttendanceResult} to allow targeted shortage
 * reporting without recomputing the full subject result.
 *
 * <p>All unit values use BigDecimal to support fractional contributions from
 * policy status mappings (e.g., HALF_DAY = 0.5 units).
 *
 * <h3>Invariants (must hold for all valid ShortageAnalysis instances)</h3>
 * <ul>
 *   <li>{@code unitsShort >= 0}</li>
 *   <li>{@code unitsSurplus >= 0}</li>
 *   <li>Exactly one of unitsShort or unitsSurplus is zero (the other is non-negative)</li>
 *   <li>If {@code isInShortage}: {@code unitsShort > 0} and {@code unitsSurplus == 0}</li>
 *   <li>If NOT in shortage: {@code unitsShort == 0} and {@code unitsSurplus >= 0}</li>
 * </ul>
 */
public record ShortageAnalysis(
    UUID studentId,
    UUID subjectId,
    BigDecimal conductedUnits,
    BigDecimal attendedUnits,
    BigDecimal attendancePercentage,
    BigDecimal targetPercentage,
    boolean isInShortage,
    BigDecimal unitsShort,
    BigDecimal unitsSurplus,
    Instant computedAt
) {
    public ShortageAnalysis {
        Objects.requireNonNull(studentId, "studentId must not be null");
        Objects.requireNonNull(subjectId, "subjectId must not be null");
        Objects.requireNonNull(conductedUnits, "conductedUnits must not be null");
        Objects.requireNonNull(attendedUnits, "attendedUnits must not be null");
        Objects.requireNonNull(attendancePercentage, "attendancePercentage must not be null");
        Objects.requireNonNull(targetPercentage, "targetPercentage must not be null");
        Objects.requireNonNull(unitsShort, "unitsShort must not be null");
        Objects.requireNonNull(unitsSurplus, "unitsSurplus must not be null");
        Objects.requireNonNull(computedAt, "computedAt must not be null");
        if (unitsShort.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("unitsShort must not be negative, got: " + unitsShort);
        }
        if (unitsSurplus.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("unitsSurplus must not be negative, got: " + unitsSurplus);
        }
    }
}
