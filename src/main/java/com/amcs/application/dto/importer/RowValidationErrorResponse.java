package com.amcs.application.dto.importer;

import com.amcs.domain.importer.RowValidationError;

public record RowValidationErrorResponse(
    int rowIndex,
    String columnName,
    String rejectedValue,
    String errorCode,
    String errorMessage
) {
    public static RowValidationErrorResponse from(RowValidationError error) {
        return new RowValidationErrorResponse(
            error.rowIndex(),
            error.columnName(),
            error.rejectedValue(),
            error.errorCode(),
            error.errorMessage()
        );
    }
}
