package com.amcs.session;

import com.amcs.timetable.TimetableEntry;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "class_sessions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"timetable_entry_id", "session_date"}))
public class ClassSession {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "timetable_entry_id", nullable = false)
    private TimetableEntry timetableEntry;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(nullable = false, length = 20)
    private String status = "OPEN";  // OPEN | SUBMITTED

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "submitted_at")
    private Instant submittedAt;

    public Long getId() { return id; }
    public TimetableEntry getTimetableEntry() { return timetableEntry; }
    public LocalDate getSessionDate() { return sessionDate; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setId(Long id) { this.id = id; }
    public void setTimetableEntry(TimetableEntry timetableEntry) { this.timetableEntry = timetableEntry; }
    public void setSessionDate(LocalDate sessionDate) { this.sessionDate = sessionDate; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
}
