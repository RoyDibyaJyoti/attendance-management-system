package com.amcs.domain.importer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root representing the lifecycle, metrics, and error diagnostics of a bulk spreadsheet import job.
 *
 * <p><strong>Pure Domain Isolation:</strong> Contains zero file stream, Apache POI, or Spring dependencies.
 * Encapsulates the import lifecycle state transitions:
 * <pre>
 *   SUBMITTED ──► STAGED_CLEAN   ──► COMMITTED
 *             ──► STAGED_PARTIAL ──► COMMITTED
 *             ──► REJECTED       ──► DISCARDED
 *             ──► DISCARDED
 * </pre>
 */
public class ImportJob {

    private final UUID id;
    private final ImportType importType;
    private final ImportMode importMode;
    private ImportStatus status;
    private final String originalFilename;
    private int totalRows;
    private int validRows;
    private int invalidRows;
    private List<RowValidationError> errors;
    private final UUID createdByUserId;
    private final Instant createdAt;
    private Instant committedAt;
    private Instant discardedAt;

    private ImportJob(
        UUID id,
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
        Instant discardedAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.importType = Objects.requireNonNull(importType, "importType must not be null");
        this.importMode = Objects.requireNonNull(importMode, "importMode must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.originalFilename = Objects.requireNonNull(originalFilename, "originalFilename must not be null").trim();
        if (this.originalFilename.isEmpty()) {
            throw new IllegalArgumentException("originalFilename must not be blank");
        }
        if (totalRows < 0 || validRows < 0 || invalidRows < 0) {
            throw new IllegalArgumentException("Row counts cannot be negative");
        }
        this.totalRows = totalRows;
        this.validRows = validRows;
        this.invalidRows = invalidRows;
        this.errors = errors != null ? Collections.unmodifiableList(new ArrayList<>(errors)) : List.of();
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "createdByUserId must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.committedAt = committedAt;
        this.discardedAt = discardedAt;
    }

    /**
     * Factory to instantiate a newly submitted import job.
     */
    public static ImportJob create(
        UUID id,
        ImportType importType,
        ImportMode importMode,
        String originalFilename,
        UUID createdByUserId,
        Instant createdAt
    ) {
        return new ImportJob(
            id,
            importType,
            importMode,
            ImportStatus.SUBMITTED,
            originalFilename,
            0,
            0,
            0,
            List.of(),
            createdByUserId,
            createdAt,
            null,
            null
        );
    }

    /**
     * Reconstitutes an ImportJob from persistence storage.
     */
    public static ImportJob reconstitute(
        UUID id,
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
        Instant discardedAt
    ) {
        return new ImportJob(
            id,
            importType,
            importMode,
            status,
            originalFilename,
            totalRows,
            validRows,
            invalidRows,
            errors,
            createdByUserId,
            createdAt,
            committedAt,
            discardedAt
        );
    }

    /**
     * Transitions the job from SUBMITTED to STAGED_CLEAN, STAGED_PARTIAL, or REJECTED
     * depending on row counts, error presence, and the configured {@link ImportMode}.
     *
     * @param totalRows total data rows evaluated in the spreadsheet
     * @param validRows count of rows satisfying all domain and referential invariants
     * @param errors    itemized validation errors discovered during parsing
     */
    public void stage(int totalRows, int validRows, List<RowValidationError> errors) {
        if (status != ImportStatus.SUBMITTED) {
            throw new IllegalStateException("Only jobs in SUBMITTED state can be staged. Current status: " + status);
        }
        if (totalRows < 0) {
            throw new IllegalArgumentException("totalRows cannot be negative: " + totalRows);
        }
        if (validRows < 0) {
            throw new IllegalArgumentException("validRows cannot be negative: " + validRows);
        }
        if (validRows > totalRows) {
            throw new IllegalArgumentException("validRows (" + validRows + ") cannot exceed totalRows (" + totalRows + ")");
        }

        List<RowValidationError> errorList = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
        this.totalRows = totalRows;
        this.validRows = validRows;
        this.invalidRows = errorList.size();
        this.errors = Collections.unmodifiableList(errorList);

        if (totalRows == 0 || validRows == 0) {
            this.status = ImportStatus.REJECTED;
            return;
        }

        if (errorList.isEmpty()) {
            this.status = ImportStatus.STAGED_CLEAN;
        } else if (importMode == ImportMode.FAIL_FAST) {
            this.status = ImportStatus.REJECTED;
        } else {
            this.status = ImportStatus.STAGED_PARTIAL;
        }
    }

    /**
     * Atomically marks the staged job as COMMITTED.
     *
     * @param timestamp timestamp when the batch commit succeeded
     */
    public void commit(Instant timestamp) {
        Objects.requireNonNull(timestamp, "committedAt timestamp must not be null");
        if (status == ImportStatus.COMMITTED) {
            throw new IllegalStateException("Import job has already been committed at: " + committedAt);
        }
        if (status == ImportStatus.DISCARDED) {
            throw new IllegalStateException("Cannot commit a discarded import job");
        }
        if (status == ImportStatus.REJECTED) {
            throw new IllegalStateException("Cannot commit a rejected import job");
        }
        if (status == ImportStatus.SUBMITTED) {
            throw new IllegalStateException("Cannot commit an import job before it has been staged and validated");
        }
        if (validRows <= 0) {
            throw new IllegalStateException("Cannot commit an import job with zero valid rows");
        }

        this.status = ImportStatus.COMMITTED;
        this.committedAt = timestamp;
    }

    /**
     * Marks the job as DISCARDED, canceling any pending commit.
     *
     * @param timestamp timestamp when the job was discarded
     */
    public void discard(Instant timestamp) {
        Objects.requireNonNull(timestamp, "discardedAt timestamp must not be null");
        if (status == ImportStatus.COMMITTED) {
            throw new IllegalStateException("Cannot discard an already committed import job");
        }
        if (status == ImportStatus.DISCARDED) {
            return; // Idempotent
        }

        this.status = ImportStatus.DISCARDED;
        this.discardedAt = timestamp;
    }

    // Getters and inquiry predicates

    public UUID getId() {
        return id;
    }

    public ImportType getImportType() {
        return importType;
    }

    public ImportMode getImportMode() {
        return importMode;
    }

    public ImportStatus getStatus() {
        return status;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public int getValidRows() {
        return validRows;
    }

    public int getInvalidRows() {
        return invalidRows;
    }

    public List<RowValidationError> getErrors() {
        return errors;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCommittedAt() {
        return committedAt;
    }

    public Instant getDiscardedAt() {
        return discardedAt;
    }

    public boolean isCommittable() {
        return status == ImportStatus.STAGED_CLEAN || status == ImportStatus.STAGED_PARTIAL;
    }

    public boolean isTerminal() {
        return status == ImportStatus.COMMITTED || status == ImportStatus.DISCARDED || status == ImportStatus.REJECTED;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
