package com.amcs.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "faculty_assignments",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_faculty_subject_section",
            columnNames = {"faculty_id", "subject_id", "section_id", "academic_period_id"}
        )
    }
)
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

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public FacultyAssignmentEntity() {}

    public FacultyAssignmentEntity(
        UUID id,
        FacultyEntity faculty,
        SubjectEntity subject,
        SectionEntity section,
        AcademicPeriodEntity academicPeriod,
        boolean isPrimary
    ) {
        this.id = id;
        this.faculty = faculty;
        this.subject = subject;
        this.section = section;
        this.academicPeriod = academicPeriod;
        this.isPrimary = isPrimary;
        this.createdAt = Instant.now();
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
    public boolean isPrimary() { return isPrimary; }
    public void setPrimary(boolean primary) { isPrimary = primary; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public UUID getFacultyId() {
        return faculty != null ? faculty.getId() : null;
    }

    public UUID getSubjectId() {
        return subject != null ? subject.getId() : null;
    }

    public UUID getSectionId() {
        return section != null ? section.getId() : null;
    }

    public UUID getAcademicPeriodId() {
        return academicPeriod != null ? academicPeriod.getId() : null;
    }
}
