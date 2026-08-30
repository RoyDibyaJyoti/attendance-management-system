package com.amcs.domain.calculation.result;

import com.amcs.domain.academic.ComponentType;
import java.util.Objects;

/**
 * Wraps a subject attendance result with its component type label.
 * Used as part of an {@link IntegratedSubjectAttendanceResult} to clearly
 * identify which result belongs to the theory stream and which to the lab stream.
 */
public record ComponentAttendanceResult(
    ComponentType componentType,
    SubjectAttendanceResult result
) {
    public ComponentAttendanceResult {
        Objects.requireNonNull(componentType, "componentType must not be null");
        Objects.requireNonNull(result, "result must not be null");
    }
}
