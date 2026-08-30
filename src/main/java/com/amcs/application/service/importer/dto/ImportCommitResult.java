package com.amcs.application.service.importer.dto;

import com.amcs.domain.importer.ImportStatus;

import java.time.Instant;
import java.util.UUID;

public record ImportCommitResult(
    UUID jobId,
    ImportStatus status,
    int committedRows,
    Instant committedAt
) {}
