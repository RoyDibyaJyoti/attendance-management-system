package com.amcs.application.port.out.excel;

import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

/**
 * Outbound port for low-memory, streaming Excel (XLSX) parsing and header validation.
 *
 * <p>Application layer remains completely isolated from Apache POI or XML parser specifics.
 */
public interface ExcelParsingPort {

    /**
     * Inspects and validates Row 1 headers from the target spreadsheet against required and optional headers.
     *
     * @param inputStream     source spreadsheet stream
     * @param requiredHeaders list of mandatory column names
     * @param optionalHeaders list of optional column names
     * @return validation result detailing validity, column index mappings, and missing/duplicate header errors
     */
    HeaderValidationResult validateHeaders(
        InputStream inputStream,
        List<String> requiredHeaders,
        List<String> optionalHeaders
    );

    /**
     * Streams rows from the first worksheet of an XLSX spreadsheet in memory-bounded batches.
     *
     * @param inputStream   source spreadsheet stream
     * @param chunkSize     maximum rows per batch (e.g., 250)
     * @param batchConsumer consumer receiving consecutive batches of parsed rows
     */
    void parseStreaming(
        InputStream inputStream,
        int chunkSize,
        Consumer<List<ParsedRow>> batchConsumer
    );

    /**
     * Streams rows from a specific worksheet of an XLSX spreadsheet in memory-bounded batches.
     *
     * @param inputStream     source spreadsheet stream
     * @param sheetNameOrNull name of worksheet to parse, or null for the first sheet
     * @param chunkSize       maximum rows per batch (e.g., 250)
     * @param batchConsumer   consumer receiving consecutive batches of parsed rows
     */
    void parseStreaming(
        InputStream inputStream,
        String sheetNameOrNull,
        int chunkSize,
        Consumer<List<ParsedRow>> batchConsumer
    );
}
