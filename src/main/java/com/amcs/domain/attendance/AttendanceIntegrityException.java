package com.amcs.domain.attendance;

import java.util.Objects;

/**
 * Thrown when attendance calculation is executed under strict validation mode
 * and one or more integrity violations are discovered.
 */
public class AttendanceIntegrityException extends RuntimeException {

    private final AttendanceIntegrityReport report;

    public AttendanceIntegrityException(AttendanceIntegrityReport report) {
        super("Attendance integrity violation(s) detected: " + formatSummary(report));
        this.report = Objects.requireNonNull(report, "report must not be null");
    }

    public AttendanceIntegrityReport getReport() {
        return report;
    }

    private static String formatSummary(AttendanceIntegrityReport report) {
        if (report == null) return "Unknown violations";
        return report.violations().stream()
            .map(v -> "[%s on session %s: %s]".formatted(v.type(), v.sessionId(), v.message()))
            .reduce((a, b) -> a + ", " + b)
            .orElse("None");
    }
}
