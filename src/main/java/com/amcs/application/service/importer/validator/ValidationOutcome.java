package com.amcs.application.service.importer.validator;

import com.amcs.domain.importer.RowValidationError;

import java.util.Optional;

public record ValidationOutcome<T>(
    T payload,
    RowValidationError error
) {
    public static <T> ValidationOutcome<T> valid(T payload) {
        return new ValidationOutcome<>(payload, null);
    }

    public static <T> ValidationOutcome<T> invalid(RowValidationError error) {
        return new ValidationOutcome<>(null, error);
    }

    public boolean isValid() {
        return error == null;
    }

    public Optional<T> getPayload() {
        return Optional.ofNullable(payload);
    }

    public Optional<RowValidationError> getError() {
        return Optional.ofNullable(error);
    }
}
