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
    name = "overall_attendance_policies",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_overall_policy_name_version", columnNames = {"name", "version"})
    }
)
public class OverallAttendancePolicyEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "aggregation_strategy", nullable = false, length = 40)
    private String aggregationStrategy;

    @Column(name = "minimum_threshold_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal minimumThresholdPercentage;

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

    public OverallAttendancePolicyEntity() {}

    public OverallAttendancePolicyEntity(
        UUID id, String name, int version, String aggregationStrategy,
        BigDecimal minimumThresholdPercentage, Instant effectiveFrom, Instant effectiveTo, boolean active
    ) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.aggregationStrategy = aggregationStrategy;
        this.minimumThresholdPercentage = minimumThresholdPercentage;
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
    public String getAggregationStrategy() { return aggregationStrategy; }
    public void setAggregationStrategy(String aggregationStrategy) { this.aggregationStrategy = aggregationStrategy; }
    public BigDecimal getMinimumThresholdPercentage() { return minimumThresholdPercentage; }
    public void setMinimumThresholdPercentage(BigDecimal minimumThresholdPercentage) { this.minimumThresholdPercentage = minimumThresholdPercentage; }
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
