package com.amcs.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "enrollments")
public class EnrollmentEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "section_id", nullable = false)
    private UUID sectionId;

    @Column(name = "enrollment_start", nullable = false)
    private LocalDate enrollmentStart;

    @Column(name = "enrollment_end")
    private LocalDate enrollmentEnd;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public EnrollmentEntity() {}

    public EnrollmentEntity(UUID id, UUID studentId, UUID sectionId, LocalDate enrollmentStart, LocalDate enrollmentEnd, String status) {
        this.id = id;
        this.studentId = studentId;
        this.sectionId = sectionId;
        this.enrollmentStart = enrollmentStart;
        this.enrollmentEnd = enrollmentEnd;
        this.status = status != null ? status : "ACTIVE";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID studentId) { this.studentId = studentId; }
    public UUID getSectionId() { return sectionId; }
    public void setSectionId(UUID sectionId) { this.sectionId = sectionId; }
    public LocalDate getEnrollmentStart() { return enrollmentStart; }
    public void setEnrollmentStart(LocalDate enrollmentStart) { this.enrollmentStart = enrollmentStart; }
    public LocalDate getEnrollmentEnd() { return enrollmentEnd; }
    public void setEnrollmentEnd(LocalDate enrollmentEnd) { this.enrollmentEnd = enrollmentEnd; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
