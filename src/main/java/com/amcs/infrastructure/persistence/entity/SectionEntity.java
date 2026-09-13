package com.amcs.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "sections",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_section_name_period", columnNames = {"department_id", "academic_period_id", "name"})
    }
)
public class SectionEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @Column(name = "academic_period_id", nullable = false)
    private UUID academicPeriodId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    public SectionEntity() {}

    public SectionEntity(UUID id, String name, UUID departmentId, UUID academicPeriodId) {
        this.id = id;
        this.name = name;
        this.departmentId = departmentId;
        this.academicPeriodId = academicPeriodId;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.isActive = true;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }
    public UUID getAcademicPeriodId() { return academicPeriodId; }
    public void setAcademicPeriodId(UUID academicPeriodId) { this.academicPeriodId = academicPeriodId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
