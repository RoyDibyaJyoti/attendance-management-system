package com.amcs.lab;

import com.amcs.section.Section;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "lab_batches", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "section_id"}))
public class LabBatch {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getName() { return name; }
    public Section getSection() { return section; }
    public Instant getCreatedAt() { return createdAt; }
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setSection(Section section) { this.section = section; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
