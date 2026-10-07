package com.amcs.subject;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "subjects")
public class Subject {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 10)
    private String type;  // THEORY | LAB

    @Column(nullable = false)
    private int credits = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getType() { return type; }
    public int getCredits() { return credits; }
    public Instant getCreatedAt() { return createdAt; }
    public void setId(Long id) { this.id = id; }
    public void setCode(String code) { this.code = code; }
    public void setName(String name) { this.name = name; }
    public void setType(String type) { this.type = type; }
    public void setCredits(int credits) { this.credits = credits; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
