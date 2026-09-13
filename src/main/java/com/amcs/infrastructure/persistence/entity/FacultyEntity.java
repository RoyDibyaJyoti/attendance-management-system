package com.amcs.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "faculty")
public class FacultyEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "employee_id", nullable = false, length = 30, unique = true)
    private String employeeId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "email", nullable = false, length = 100, unique = true)
    private String email;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    public FacultyEntity() {}

    public FacultyEntity(UUID id, String employeeId, String name, String email, UUID departmentId) {
        this.id = id;
        this.employeeId = employeeId;
        this.name = name;
        this.email = email;
        this.departmentId = departmentId;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.isActive = true;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
