package com.amcs.infrastructure.excel.parser;

import com.amcs.application.port.out.excel.HeaderValidationResult;
import com.amcs.domain.importer.RowValidationError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Strict validator for spreadsheet header rows (Row 1).
 */
public final class HeaderValidator {

    private HeaderValidator() {}

    /**
     * Validates an ordered list of header strings from Row 1 against schema expectations.
     *
     * @param rawHeaders      ordered list of cell values found on Row 1
     * @param requiredHeaders list of mandatory column names
     * @param optionalHeaders list of optional column names
     * @return structured validation result with column index mappings and itemized errors
     */
    public static HeaderValidationResult validate(
        List<String> rawHeaders,
        List<String> requiredHeaders,
        List<String> optionalHeaders
    ) {
        Map<String, Integer> headerToColIndex = new HashMap<>();
        List<String> missingHeaders = new ArrayList<>();
        List<String> duplicateHeaders = new ArrayList<>();
        List<String> unexpectedHeaders = new ArrayList<>();
        List<RowValidationError> errors = new ArrayList<>();

        if (rawHeaders == null || rawHeaders.isEmpty()) {
            errors.add(new RowValidationError(1, "HeaderRow", "", "EMPTY_HEADER_ROW", "Header row is missing or empty"));
            return HeaderValidationResult.invalid(headerToColIndex, requiredHeaders, duplicateHeaders, unexpectedHeaders, errors);
        }

        Set<String> seenHeaders = new HashSet<>();
        for (int colIdx = 0; colIdx < rawHeaders.size(); colIdx++) {
            String raw = rawHeaders.get(colIdx);
            if (raw == null || raw.trim().isEmpty()) {
                errors.add(new RowValidationError(1, "Column " + (colIdx + 1), "", "EMPTY_HEADER", "Column header at index " + (colIdx + 1) + " is empty"));
                continue;
            }

            String normalized = raw.trim().toLowerCase();
            if (seenHeaders.contains(normalized)) {
                duplicateHeaders.add(raw.trim());
                errors.add(new RowValidationError(
                    1, raw.trim(), raw.trim(), "DUPLICATE_HEADER", "Duplicate column header: '" + raw.trim() + "'"
                ));
            } else {
                seenHeaders.add(normalized);
                headerToColIndex.put(normalized, colIdx);
            }
        }

        // Check required headers
        if (requiredHeaders != null) {
            for (String req : requiredHeaders) {
                String reqNorm = req.trim().toLowerCase();
                if (!headerToColIndex.containsKey(reqNorm)) {
                    missingHeaders.add(req);
                    errors.add(new RowValidationError(
                        1, req, "", "MISSING_REQUIRED_HEADER", "Required column header missing: '" + req + "'"
                    ));
                }
            }
        }

        // Check for unexpected headers if optional headers are specified
        Set<String> allowedNorm = new HashSet<>();
        if (requiredHeaders != null) {
            for (String req : requiredHeaders) {
                allowedNorm.add(req.trim().toLowerCase());
            }
        }
        if (optionalHeaders != null) {
            for (String opt : optionalHeaders) {
                allowedNorm.add(opt.trim().toLowerCase());
            }
        }

        for (String raw : rawHeaders) {
            if (raw != null && !raw.trim().isEmpty()) {
                String norm = raw.trim().toLowerCase();
                if (!allowedNorm.isEmpty() && !allowedNorm.contains(norm)) {
                    unexpectedHeaders.add(raw.trim());
                }
            }
        }

        boolean isValid = errors.isEmpty();
        if (isValid) {
            return HeaderValidationResult.valid(headerToColIndex);
        } else {
            return HeaderValidationResult.invalid(headerToColIndex, missingHeaders, duplicateHeaders, unexpectedHeaders, errors);
        }
    }
}
