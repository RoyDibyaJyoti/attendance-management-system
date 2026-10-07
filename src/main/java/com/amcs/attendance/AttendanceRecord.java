package com.amcs.attendance;

import com.amcs.session.ClassSession;
import com.amcs.student.Student;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "attendance_records",
       uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "student_id"}))
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ClassSession session;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false, length = 10)
    private String status; // 'PRESENT' | 'ABSENT'

    @Column(name = "marked_at", nullable = false)
    private Instant markedAt = Instant.now();

    public AttendanceRecord() {}

    public AttendanceRecord(ClassSession session, Student student, String status) {
        this.session = session;
        this.student = student;
        this.status = status;
        this.markedAt = Instant.now();
    }

    public Long getId() { return id; }
    public ClassSession getSession() { return session; }
    public Student getStudent() { return student; }
    public String getStatus() { return status; }
    public Instant getMarkedAt() { return markedAt; }

    public void setId(Long id) { this.id = id; }
    public void setSession(ClassSession session) { this.session = session; }
    public void setStudent(Student student) { this.student = student; }
    public void setStatus(String status) { this.status = status; }
    public void setMarkedAt(Instant markedAt) { this.markedAt = markedAt; }
}
