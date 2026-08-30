package com.amcs.domain.importer;

import java.util.Objects;

/**
 * Immutable value object representing a specific validation failure on a row within an import file.
 *
 * @param rowIndex      1-based spreadsheet row index
 * @param columnName    Name of the offending column or header
 * @param rejectedValue String representation of the invalid cell value
 * @param errorCode     Machine-readable error identifier (e.g., INVALID_EMAIL, MISSING_REQUIRED_FIELD)
 * @param errorMessage  Human-readable description of the error
 */
public record RowValidationError(
    int rowIndex,
    String columnName,
    String rejectedValue,
    String errorCode,
    String errorMessage
) {
    public RowValidationError {
        if (rowIndex < 1) {
            throw new IllegalArgumentException("Row index must be >= 1, was: " + rowIndex);
        }
        errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null").trim();
        if (errorCode.isEmpty()) {
            throw new IllegalArgumentException("errorCode must not be blank");
        }
        errorMessage = Objects.requireNonNull(errorMessage, "errorMessage must not be null").trim();
        if (errorMessage.isEmpty()) {
            throw new IllegalArgumentException("errorMessage must not be blank");
        }
        columnName = columnName != null ? columnName.trim() : "";
        rejectedValue = rejectedValue != null ? rejectedValue.trim() : "";
    }
}
