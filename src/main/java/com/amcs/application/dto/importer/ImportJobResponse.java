package com.amcs.application.dto.importer;

import com.amcs.application.service.importer.dto.ImportSubmissionResult;
import com.amcs.domain.importer.ImportMode;
import com.amcs.domain.importer.ImportStatus;
import com.amcs.domain.importer.ImportType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record ImportJobResponse(
    UUID jobId,
    ImportType importType,
    ImportMode importMode,
    ImportStatus status,
    String originalFilename,
    int totalRows,
    int validRows,
    int invalidRows,
    List<RowValidationErrorResponse> errors,
    UUID createdByUserId,
    Instant createdAt,
    Instant committedAt,
    Instant discardedAt,
    boolean isCommittable
) {
    public static ImportJobResponse from(ImportSubmissionResult result) {
        List<RowValidationErrorResponse> errorResponses = result.errors() != null
            ? result.errors().stream().map(RowValidationErrorResponse::from).collect(Collectors.toList())
            : List.of();

        return new ImportJobResponse(
            result.jobId(),
            result.importType(),
            result.importMode(),
            result.status(),
            result.originalFilename(),
            result.totalRows(),
            result.validRows(),
            result.invalidRows(),
            errorResponses,
            result.createdByUserId(),
            result.createdAt(),
            result.committedAt(),
            result.discardedAt(),
            result.isCommittable()
        );
    }
}
