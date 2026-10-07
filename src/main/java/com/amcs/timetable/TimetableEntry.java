package com.amcs.timetable;

import com.amcs.faculty.Faculty;
import com.amcs.lab.LabBatch;
import com.amcs.section.Section;
import com.amcs.subject.Subject;
import com.amcs.term.AcademicTerm;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "timetable_entries")
public class TimetableEntry {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private AcademicTerm term;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @Column(name = "day_of_week", nullable = false, length = 10)
    private String dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(length = 50)
    private String room;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lab_batch_id")
    private LabBatch labBatch;  // null = theory (all students)

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public AcademicTerm getTerm() { return term; }
    public Section getSection() { return section; }
    public Subject getSubject() { return subject; }
    public Faculty getFaculty() { return faculty; }
    public String getDayOfWeek() { return dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getRoom() { return room; }
    public LabBatch getLabBatch() { return labBatch; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setId(Long id) { this.id = id; }
    public void setTerm(AcademicTerm term) { this.term = term; }
    public void setSection(Section section) { this.section = section; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public void setFaculty(Faculty faculty) { this.faculty = faculty; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public void setRoom(String room) { this.room = room; }
    public void setLabBatch(LabBatch labBatch) { this.labBatch = labBatch; }
    public void setActive(boolean active) { this.active = active; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
