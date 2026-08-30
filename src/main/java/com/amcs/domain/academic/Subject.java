package com.amcs.domain.academic;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a subject or course offered in the institution.
 *
 * <p>{@code creditHours} is required for overall attendance strategies that weight
 * by credit value ({@link com.amcs.domain.policy.OverallAggregationStrategy#WEIGHTED_BY_CREDITS}).
 * Set to 0 if the institution does not use credit hours, but document this explicitly.
 *
 * <p>A subject does NOT embed its AttendancePolicy directly. The policy is resolved
 * at calculation time via the service layer to support policy versioning.
 */
public record Subject(
    UUID id,
    String name,
    String code,
    CourseType courseType,
    int creditHours
) {
    public Subject {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(courseType, "courseType must not be null");
        if (name.isBlank()) throw new IllegalArgumentException("name must not be blank");
        if (code.isBlank()) throw new IllegalArgumentException("code must not be blank");
        if (creditHours < 0) throw new IllegalArgumentException(
            "creditHours must not be negative, got: " + creditHours);
    }
}
