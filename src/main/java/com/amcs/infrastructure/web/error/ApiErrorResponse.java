package com.amcs.infrastructure.web.error;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
    Instant timestamp,
    int status,
    String code,
    String message,
    String path,
    List<ValidationErrorDetail> details
) {
    public static ApiErrorResponse of(int status, String code, String message, String path) {
        return new ApiErrorResponse(Instant.now(), status, code, message, path, List.of());
    }

    public static ApiErrorResponse withDetails(
        int status, String code, String message, String path, List<ValidationErrorDetail> details
    ) {
        return new ApiErrorResponse(Instant.now(), status, code, message, path, details);
    }
}
