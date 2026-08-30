package com.amcs.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "sessions")
public class SessionEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "section_id", nullable = false)
    private UUID sectionId;

    @Column(name = "conducted_by_faculty_id", nullable = false)
    private UUID conductedByFacultyId;

    @Column(name = "academic_period_id", nullable = false)
    private UUID academicPeriodId;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "session_type", nullable = false, length = 20)
    private String sessionType;

    @Column(name = "planned_units", nullable = false)
    private int plannedUnits;

    @Column(name = "conducted_units", nullable = false)
    private int conductedUnits;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "lab_group_id")
    private UUID labGroupId;

    @Column(name = "replaced_by_session_id")
    private UUID replacedBySessionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @jakarta.persistence.Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    public SessionEntity() {}

    public SessionEntity(
        UUID id, UUID subjectId, UUID sectionId, UUID conductedByFacultyId, UUID academicPeriodId,
        LocalDate sessionDate, String sessionType, int plannedUnits, int conductedUnits, String status,
        UUID labGroupId, UUID replacedBySessionId
    ) {
        this.id = id;
        this.subjectId = subjectId;
        this.sectionId = sectionId;
        this.conductedByFacultyId = conductedByFacultyId;
        this.academicPeriodId = academicPeriodId;
        this.sessionDate = sessionDate;
        this.sessionType = sessionType;
        this.plannedUnits = plannedUnits;
        this.conductedUnits = conductedUnits;
        this.status = status;
        this.labGroupId = labGroupId;
        this.replacedBySessionId = replacedBySessionId;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getSubjectId() { return subjectId; }
    public void setSubjectId(UUID subjectId) { this.subjectId = subjectId; }
    public UUID getSectionId() { return sectionId; }
    public void setSectionId(UUID sectionId) { this.sectionId = sectionId; }
    public UUID getConductedByFacultyId() { return conductedByFacultyId; }
    public void setConductedByFacultyId(UUID conductedByFacultyId) { this.conductedByFacultyId = conductedByFacultyId; }
    public UUID getAcademicPeriodId() { return academicPeriodId; }
    public void setAcademicPeriodId(UUID academicPeriodId) { this.academicPeriodId = academicPeriodId; }
    public LocalDate getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDate sessionDate) { this.sessionDate = sessionDate; }
    public String getSessionType() { return sessionType; }
    public void setSessionType(String sessionType) { this.sessionType = sessionType; }
    public int getPlannedUnits() { return plannedUnits; }
    public void setPlannedUnits(int plannedUnits) { this.plannedUnits = plannedUnits; }
    public int getConductedUnits() { return conductedUnits; }
    public void setConductedUnits(int conductedUnits) { this.conductedUnits = conductedUnits; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public UUID getLabGroupId() { return labGroupId; }
    public void setLabGroupId(UUID labGroupId) { this.labGroupId = labGroupId; }
    public UUID getReplacedBySessionId() { return replacedBySessionId; }
    public void setReplacedBySessionId(UUID replacedBySessionId) { this.replacedBySessionId = replacedBySessionId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
