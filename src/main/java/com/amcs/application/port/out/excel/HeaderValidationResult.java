package com.amcs.application.port.out.excel;

import com.amcs.domain.importer.RowValidationError;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Result of validating spreadsheet column headers against a target schema.
 */
public record HeaderValidationResult(
    boolean isValid,
    Map<String, Integer> headerToColumnIndex,
    List<String> missingHeaders,
    List<String> duplicateHeaders,
    List<String> unexpectedHeaders,
    List<RowValidationError> errors
) {
    public HeaderValidationResult {
        headerToColumnIndex = headerToColumnIndex != null ? Collections.unmodifiableMap(headerToColumnIndex) : Map.of();
        missingHeaders = missingHeaders != null ? Collections.unmodifiableList(missingHeaders) : List.of();
        duplicateHeaders = duplicateHeaders != null ? Collections.unmodifiableList(duplicateHeaders) : List.of();
        unexpectedHeaders = unexpectedHeaders != null ? Collections.unmodifiableList(unexpectedHeaders) : List.of();
        errors = errors != null ? Collections.unmodifiableList(errors) : List.of();
    }

    public static HeaderValidationResult valid(Map<String, Integer> headerToColumnIndex) {
        return new HeaderValidationResult(true, headerToColumnIndex, List.of(), List.of(), List.of(), List.of());
    }

    public static HeaderValidationResult invalid(
        Map<String, Integer> headerToColumnIndex,
        List<String> missingHeaders,
        List<String> duplicateHeaders,
        List<String> unexpectedHeaders,
        List<RowValidationError> errors
    ) {
        return new HeaderValidationResult(false, headerToColumnIndex, missingHeaders, duplicateHeaders, unexpectedHeaders, errors);
    }
}
