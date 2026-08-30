package com.amcs.application.dto.importer;

import com.amcs.application.service.importer.dto.ImportCommitResult;
import com.amcs.domain.importer.ImportStatus;

import java.time.Instant;
import java.util.UUID;

public record ImportCommitResponse(
    UUID jobId,
    ImportStatus status,
    int committedRows,
    Instant committedAt
) {
    public static ImportCommitResponse from(ImportCommitResult result) {
        return new ImportCommitResponse(
            result.jobId(),
            result.status(),
            result.committedRows(),
            result.committedAt()
        );
    }
}
