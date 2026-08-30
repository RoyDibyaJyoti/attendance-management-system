package com.amcs.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "import_job_errors")
public class ImportJobErrorEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private ImportJobEntity job;

    @Column(name = "row_index", nullable = false)
    private int rowIndex;

    @Column(name = "column_name", nullable = false, length = 100)
    private String columnName;

    @Column(name = "rejected_value", nullable = false, columnDefinition = "TEXT")
    private String rejectedValue;

    @Column(name = "error_code", nullable = false, length = 100)
    private String errorCode;

    @Column(name = "error_message", nullable = false, columnDefinition = "TEXT")
    private String errorMessage;

    public ImportJobErrorEntity() {}

    public ImportJobErrorEntity(
        UUID id,
        ImportJobEntity job,
        int rowIndex,
        String columnName,
        String rejectedValue,
        String errorCode,
        String errorMessage
    ) {
        this.id = id;
        this.job = job;
        this.rowIndex = rowIndex;
        this.columnName = columnName != null ? columnName : "";
        this.rejectedValue = rejectedValue != null ? rejectedValue : "";
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ImportJobEntity getJob() {
        return job;
    }

    public void setJob(ImportJobEntity job) {
        this.job = job;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public void setRowIndex(int rowIndex) {
        this.rowIndex = rowIndex;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getRejectedValue() {
        return rejectedValue;
    }

    public void setRejectedValue(String rejectedValue) {
        this.rejectedValue = rejectedValue;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
