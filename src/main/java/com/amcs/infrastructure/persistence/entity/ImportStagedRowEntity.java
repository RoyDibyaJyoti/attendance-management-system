package com.amcs.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "import_staged_rows")
public class ImportStagedRowEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "row_index", nullable = false)
    private int rowIndex;

    @Column(name = "row_type", nullable = false, length = 50)
    private String rowType;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    public ImportStagedRowEntity() {}

    public ImportStagedRowEntity(UUID id, UUID jobId, int rowIndex, String rowType, String payloadJson) {
        this.id = id;
        this.jobId = jobId;
        this.rowIndex = rowIndex;
        this.rowType = rowType;
        this.payloadJson = payloadJson;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }

    public int getRowIndex() { return rowIndex; }
    public void setRowIndex(int rowIndex) { this.rowIndex = rowIndex; }

    public String getRowType() { return rowType; }
    public void setRowType(String rowType) { this.rowType = rowType; }

    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
}
