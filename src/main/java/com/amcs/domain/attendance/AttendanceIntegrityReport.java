package com.amcs.domain.attendance;

import java.util.List;
import java.util.Objects;

/**
 * Report detailing all data integrity violations discovered in an attendance dataset.
 */
public record AttendanceIntegrityReport(
    List<IntegrityViolation> violations
) {
    public AttendanceIntegrityReport {
        Objects.requireNonNull(violations, "violations must not be null");
        violations = List.copyOf(violations);
    }

    public static AttendanceIntegrityReport clean() {
        return new AttendanceIntegrityReport(List.of());
    }

    public boolean isValid() {
        return violations.isEmpty();
    }

    public boolean hasViolations() {
        return !violations.isEmpty();
    }

    public int violationCount() {
        return violations.size();
    }

    public List<IntegrityViolation> getViolationsForType(IntegrityViolationType type) {
        Objects.requireNonNull(type, "type must not be null");
        return violations.stream()
            .filter(v -> v.type() == type)
            .toList();
    }

    public boolean hasViolationType(IntegrityViolationType type) {
        Objects.requireNonNull(type, "type must not be null");
        return violations.stream().anyMatch(v -> v.type() == type);
    }
}
