package com.amcs.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "attendance_policies",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_policy_name_version", columnNames = {"name", "version"})
    }
)
public class AttendancePolicyEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "minimum_threshold_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal minimumThresholdPercentage;

    @Column(name = "status_contributions_json", nullable = false, columnDefinition = "TEXT")
    private String statusContributionsJson;

    @Column(name = "missing_record_strategy", nullable = false, length = 40)
    private String missingRecordStrategy;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public AttendancePolicyEntity() {}

    public AttendancePolicyEntity(
        UUID id, String name, int version, BigDecimal minimumThresholdPercentage,
        String statusContributionsJson, String missingRecordStrategy,
        Instant effectiveFrom, Instant effectiveTo, boolean active
    ) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.minimumThresholdPercentage = minimumThresholdPercentage;
        this.statusContributionsJson = statusContributionsJson;
        this.missingRecordStrategy = missingRecordStrategy;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.active = active;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public BigDecimal getMinimumThresholdPercentage() { return minimumThresholdPercentage; }
    public void setMinimumThresholdPercentage(BigDecimal minimumThresholdPercentage) { this.minimumThresholdPercentage = minimumThresholdPercentage; }
    public String getStatusContributionsJson() { return statusContributionsJson; }
    public void setStatusContributionsJson(String statusContributionsJson) { this.statusContributionsJson = statusContributionsJson; }
    public String getMissingRecordStrategy() { return missingRecordStrategy; }
    public void setMissingRecordStrategy(String missingRecordStrategy) { this.missingRecordStrategy = missingRecordStrategy; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(Instant effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public Instant getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(Instant effectiveTo) { this.effectiveTo = effectiveTo; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
