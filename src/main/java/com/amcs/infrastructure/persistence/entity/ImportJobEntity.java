package com.amcs.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "import_jobs")
public class ImportJobEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "import_type", nullable = false, length = 50)
    private String importType;

    @Column(name = "import_mode", nullable = false, length = 50)
    private String importMode;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "total_rows", nullable = false)
    private int totalRows;

    @Column(name = "valid_rows", nullable = false)
    private int validRows;

    @Column(name = "invalid_rows", nullable = false)
    private int invalidRows;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "committed_at")
    private Instant committedAt;

    @Column(name = "discarded_at")
    private Instant discardedAt;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("rowIndex ASC")
    private List<ImportJobErrorEntity> errors = new ArrayList<>();

    public ImportJobEntity() {}

    public ImportJobEntity(
        UUID id,
        String importType,
        String importMode,
        String status,
        String originalFilename,
        int totalRows,
        int validRows,
        int invalidRows,
        UUID createdByUserId,
        Instant createdAt,
        Instant committedAt,
        Instant discardedAt
    ) {
        this.id = id;
        this.importType = importType;
        this.importMode = importMode;
        this.status = status;
        this.originalFilename = originalFilename;
        this.totalRows = totalRows;
        this.validRows = validRows;
        this.invalidRows = invalidRows;
        this.createdByUserId = createdByUserId;
        this.createdAt = createdAt;
        this.committedAt = committedAt;
        this.discardedAt = discardedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getImportType() {
        return importType;
    }

    public void setImportType(String importType) {
        this.importType = importType;
    }

    public String getImportMode() {
        return importMode;
    }

    public void setImportMode(String importMode) {
        this.importMode = importMode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getValidRows() {
        return validRows;
    }

    public void setValidRows(int validRows) {
        this.validRows = validRows;
    }

    public int getInvalidRows() {
        return invalidRows;
    }

    public void setInvalidRows(int invalidRows) {
        this.invalidRows = invalidRows;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(UUID createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCommittedAt() {
        return committedAt;
    }

    public void setCommittedAt(Instant committedAt) {
        this.committedAt = committedAt;
    }

    public Instant getDiscardedAt() {
        return discardedAt;
    }

    public void setDiscardedAt(Instant discardedAt) {
        this.discardedAt = discardedAt;
    }

    public List<ImportJobErrorEntity> getErrors() {
        return errors;
    }

    public void setErrors(List<ImportJobErrorEntity> errors) {
        this.errors = errors;
    }

    public void addError(ImportJobErrorEntity error) {
        errors.add(error);
        error.setJob(this);
    }
}
