package com.amcs.application.port.out.excel;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable representation of a single parsed spreadsheet row.
 *
 * @param rowIndex       1-based Excel row number
 * @param valuesByHeader Map of normalized column header to cell string value
 * @param rawValues      Ordered cell string values across columns
 * @param isEmpty        Whether all cells in this row are blank/empty
 */
public record ParsedRow(
    int rowIndex,
    Map<String, String> valuesByHeader,
    List<String> rawValues,
    boolean isEmpty
) {
    public ParsedRow {
        if (rowIndex < 1) {
            throw new IllegalArgumentException("Row index must be >= 1, was: " + rowIndex);
        }
        valuesByHeader = valuesByHeader != null ? Collections.unmodifiableMap(valuesByHeader) : Map.of();
        rawValues = rawValues != null ? Collections.unmodifiableList(rawValues) : List.of();
    }

    /**
     * Retrieves the trimmed string value for a given header, or empty string if absent or null.
     */
    public String get(String header) {
        if (header == null) {
            return "";
        }
        String val = valuesByHeader.get(header.trim().toLowerCase());
        return val != null ? val.trim() : "";
    }
}
