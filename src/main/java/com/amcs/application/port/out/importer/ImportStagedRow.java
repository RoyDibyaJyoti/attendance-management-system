package com.amcs.application.port.out.importer;

import java.util.Objects;
import java.util.UUID;

/**
 * Value record representing a validated row staged for pending confirmation and atomic commit.
 *
 * @param id          unique identifier of the staged row record
 * @param jobId       parent import job identifier
 * @param rowIndex    1-based row index within original spreadsheet
 * @param rowType     entity category (STUDENTS, SESSIONS, ATTENDANCE_RECORDS)
 * @param payloadJson serialized JSON payload containing resolved entity attributes
 */
public record ImportStagedRow(
    UUID id,
    UUID jobId,
    int rowIndex,
    String rowType,
    String payloadJson
) {
    public ImportStagedRow {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(jobId, "jobId must not be null");
        if (rowIndex < 1) {
            throw new IllegalArgumentException("rowIndex must be >= 1, was: " + rowIndex);
        }
        Objects.requireNonNull(rowType, "rowType must not be null");
        Objects.requireNonNull(payloadJson, "payloadJson must not be null");
    }

    public static ImportStagedRow create(UUID jobId, int rowIndex, String rowType, String payloadJson) {
        return new ImportStagedRow(UUID.randomUUID(), jobId, rowIndex, rowType, payloadJson);
    }
}
