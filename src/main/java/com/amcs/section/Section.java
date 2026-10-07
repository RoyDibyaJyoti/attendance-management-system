package com.amcs.section;

import com.amcs.department.Department;
import com.amcs.term.AcademicTerm;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sections", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "department_id", "term_id"}))
public class Section {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private AcademicTerm term;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getName() { return name; }
    public Department getDepartment() { return department; }
    public AcademicTerm getTerm() { return term; }
    public Instant getCreatedAt() { return createdAt; }
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDepartment(Department department) { this.department = department; }
    public void setTerm(AcademicTerm term) { this.term = term; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
