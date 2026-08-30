package com.amcs.application.port.out.excel;

import com.amcs.domain.importer.RowValidationError;

import java.util.Optional;

/**
 * Result of converting a raw cell string into a typed domain value.
 *
 * @param <T> type of converted value
 */
public record ConversionResult<T>(
    T value,
    RowValidationError error
) {
    public static <T> ConversionResult<T> success(T value) {
        return new ConversionResult<>(value, null);
    }

    public static <T> ConversionResult<T> failure(RowValidationError error) {
        return new ConversionResult<>(null, error);
    }

    public boolean isSuccess() {
        return error == null;
    }

    public Optional<T> getValue() {
        return Optional.ofNullable(value);
    }

    public Optional<RowValidationError> getError() {
        return Optional.ofNullable(error);
    }
}
