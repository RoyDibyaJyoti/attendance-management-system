package com.amcs.domain.attendance;

import java.util.Objects;
import java.util.UUID;

/**
 * Encapsulates an individual integrity violation detected in attendance data.
 */
public record IntegrityViolation(
    IntegrityViolationType type,
    UUID sessionId,
    UUID studentId,
    String message
) {
    public IntegrityViolation {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(studentId, "studentId must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }
}
