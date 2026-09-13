package com.amcs.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "faculty_section_assignments")
public class FacultyAssignmentEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id", nullable = false)
    private FacultyEntity faculty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private SubjectEntity subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private SectionEntity section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_period_id", nullable = false)
    private AcademicPeriodEntity academicPeriod;

    @Column(name = "assignment_start", nullable = false)
    private LocalDate assignmentStart;

    @Column(name = "assignment_end")
    private LocalDate assignmentEnd;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public FacultyAssignmentEntity() {}

    public FacultyAssignmentEntity(
        UUID id,
        FacultyEntity faculty,
        SubjectEntity subject,
        SectionEntity section,
        AcademicPeriodEntity academicPeriod,
        LocalDate assignmentStart,
        LocalDate assignmentEnd,
        String status
    ) {
        this.id = id;
        this.faculty = faculty;
        this.subject = subject;
        this.section = section;
        this.academicPeriod = academicPeriod;
        this.assignmentStart = assignmentStart;
        this.assignmentEnd = assignmentEnd;
        this.status = status;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public FacultyEntity getFaculty() { return faculty; }
    public void setFaculty(FacultyEntity faculty) { this.faculty = faculty; }
    public SubjectEntity getSubject() { return subject; }
    public void setSubject(SubjectEntity subject) { this.subject = subject; }
    public SectionEntity getSection() { return section; }
    public void setSection(SectionEntity section) { this.section = section; }
    public AcademicPeriodEntity getAcademicPeriod() { return academicPeriod; }
    public void setAcademicPeriod(AcademicPeriodEntity academicPeriod) { this.academicPeriod = academicPeriod; }
    
    public LocalDate getAssignmentStart() { return assignmentStart; }
    public void setAssignmentStart(LocalDate assignmentStart) { this.assignmentStart = assignmentStart; }
    
    public LocalDate getAssignmentEnd() { return assignmentEnd; }
    public void setAssignmentEnd(LocalDate assignmentEnd) { this.assignmentEnd = assignmentEnd; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public UUID getFacultyId() { return faculty != null ? faculty.getId() : null; }
    public UUID getSubjectId() { return subject != null ? subject.getId() : null; }
    public UUID getSectionId() { return section != null ? section.getId() : null; }
    public UUID getAcademicPeriodId() { return academicPeriod != null ? academicPeriod.getId() : null; }
}
