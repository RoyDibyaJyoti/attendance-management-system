package com.amcs.enrollment;

import com.amcs.lab.LabBatch;
import com.amcs.section.Section;
import com.amcs.student.Student;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "enrollments", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "section_id"}))
public class Enrollment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lab_batch_id")
    private LabBatch labBatch;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public Section getSection() { return section; }
    public LabBatch getLabBatch() { return labBatch; }
    public Instant getCreatedAt() { return createdAt; }
    public void setId(Long id) { this.id = id; }
    public void setStudent(Student student) { this.student = student; }
    public void setSection(Section section) { this.section = section; }
    public void setLabBatch(LabBatch labBatch) { this.labBatch = labBatch; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
