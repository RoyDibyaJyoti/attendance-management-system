package com.amcs.application.service.importer.dto;

import com.amcs.domain.importer.ImportJob;
import com.amcs.domain.importer.ImportMode;
import com.amcs.domain.importer.ImportStatus;
import com.amcs.domain.importer.ImportType;
import com.amcs.domain.importer.RowValidationError;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ImportSubmissionResult(
    UUID jobId,
    ImportType importType,
    ImportMode importMode,
    ImportStatus status,
    String originalFilename,
    int totalRows,
    int validRows,
    int invalidRows,
    List<RowValidationError> errors,
    UUID createdByUserId,
    Instant createdAt,
    Instant committedAt,
    Instant discardedAt,
    boolean isCommittable
) {
    public static ImportSubmissionResult from(ImportJob job) {
        return new ImportSubmissionResult(
            job.getId(),
            job.getImportType(),
            job.getImportMode(),
            job.getStatus(),
            job.getOriginalFilename(),
            job.getTotalRows(),
            job.getValidRows(),
            job.getInvalidRows(),
            job.getErrors(),
            job.getCreatedByUserId(),
            job.getCreatedAt(),
            job.getCommittedAt(),
            job.getDiscardedAt(),
            job.isCommittable()
        );
    }
}
