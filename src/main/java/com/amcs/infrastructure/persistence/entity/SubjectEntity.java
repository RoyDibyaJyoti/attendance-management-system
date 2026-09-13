package com.amcs.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subjects")
public class SubjectEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "code", nullable = false, length = 20, unique = true)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "course_type", nullable = false, length = 40)
    private String courseType;

    @Column(name = "credit_hours", nullable = false)
    private int creditHours;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    public SubjectEntity() {}

    public SubjectEntity(UUID id, String code, String name, String courseType, int creditHours, UUID departmentId) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.courseType = courseType;
        this.creditHours = creditHours;
        this.departmentId = departmentId;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.isActive = true;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCourseType() { return courseType; }
    public void setCourseType(String courseType) { this.courseType = courseType; }
    public int getCreditHours() { return creditHours; }
    public void setCreditHours(int creditHours) { this.creditHours = creditHours; }
    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
