package com.amcs.attendance;

import com.amcs.enrollment.Enrollment;
import com.amcs.enrollment.EnrollmentRepository;
import com.amcs.faculty.Faculty;
import com.amcs.faculty.FacultyRepository;
import com.amcs.session.ClassSession;
import com.amcs.session.ClassSessionRepository;
import com.amcs.student.Student;
import com.amcs.student.StudentRepository;
import com.amcs.timetable.TimetableEntry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {

    private final AttendanceRecordRepository attendanceRepo;
    private final ClassSessionRepository sessionRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final FacultyRepository facultyRepo;
    private final StudentRepository studentRepo;

    public AttendanceController(AttendanceRecordRepository attendanceRepo,
                                ClassSessionRepository sessionRepo,
                                EnrollmentRepository enrollmentRepo,
                                FacultyRepository facultyRepo,
                                StudentRepository studentRepo) {
        this.attendanceRepo = attendanceRepo;
        this.sessionRepo = sessionRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.facultyRepo = facultyRepo;
        this.studentRepo = studentRepo;
    }

    public record StudentAttendanceItem(
            @NotNull Long studentId,
            @NotNull String status // "PRESENT" or "ABSENT"
    ) {}

    public record SubmitAttendanceRequest(
            @NotEmpty List<StudentAttendanceItem> records
    ) {}

    public record SubmitAttendanceResponse(
            Long sessionId,
            String status,
            Instant submittedAt,
            int totalRecords,
            int presentCount,
            int absentCount
    ) {}

    public record AttendanceRecordDto(
            Long id,
            Long studentId,
            String registrationNumber,
            String studentName,
            String status,
            Instant markedAt
    ) {}

    public record ErrorBody(String message) {}

    /**
     * Submit attendance for a class session.
     * Enforces:
     * 1. Session must exist and not already be SUBMITTED.
     * 2. Faculty authorization or ADMIN.
     * 3. No duplicate student IDs in submission.
     * 4. All submitted students must belong to the session roster.
     * 5. Atomically persists attendance records and marks session as SUBMITTED.
     */
    @PostMapping("/sessions/{sessionId}/submit")
    @Transactional
    public ResponseEntity<?> submitAttendance(
            @PathVariable Long sessionId,
            @Valid @RequestBody SubmitAttendanceRequest req,
            Authentication auth) {

        ClassSession session = sessionRepo.findById(sessionId).orElse(null);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorBody("Class session not found"));
        }

        // Prevent modification if already SUBMITTED
        if ("SUBMITTED".equalsIgnoreCase(session.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorBody("Attendance has already been submitted for this session and cannot be modified"));
        }

        TimetableEntry entry = session.getTimetableEntry();

        // Authorization check
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            String username = auth.getName();
            Faculty faculty = facultyRepo.findByUserUsername(username).orElse(null);
            if (faculty == null || !entry.getFaculty().getId().equals(faculty.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorBody("You are not authorized to submit attendance for this class"));
            }
        }

        // Determine valid roster students for this session
        Long sectionId = entry.getSection().getId();
        List<Enrollment> enrollments;
        if (entry.getLabBatch() != null) {
            enrollments = enrollmentRepo.findBySectionIdAndLabBatchId(sectionId, entry.getLabBatch().getId());
        } else {
            enrollments = enrollmentRepo.findBySectionIdOrdered(sectionId);
        }

        Map<Long, Student> rosterStudents = enrollments.stream()
                .map(Enrollment::getStudent)
                .filter(s -> s != null && s.isActive())
                .collect(Collectors.toMap(Student::getId, s -> s, (s1, s2) -> s1));

        // Validation 1: Check for duplicate student IDs in submission
        Set<Long> seenIds = new HashSet<>();
        for (StudentAttendanceItem item : req.records()) {
            if (!seenIds.add(item.studentId())) {
                return ResponseEntity.badRequest()
                        .body(new ErrorBody("Duplicate student ID in attendance batch: " + item.studentId()));
            }
        }

        // Validation 2: Ensure all students in submission belong to the roster
        for (StudentAttendanceItem item : req.records()) {
            if (!rosterStudents.containsKey(item.studentId())) {
                return ResponseEntity.badRequest()
                        .body(new ErrorBody("Student ID " + item.studentId() + " does not belong to this session roster"));
            }
            String st = item.status().toUpperCase();
            if (!"PRESENT".equals(st) && !"ABSENT".equals(st)) {
                return ResponseEntity.badRequest()
                        .body(new ErrorBody("Invalid status: " + item.status() + ". Must be PRESENT or ABSENT"));
            }
        }

        // Validation 3: Ensure all roster students are accounted for
        if (req.records().size() != rosterStudents.size()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorBody("Submission student count (" + req.records().size()
                            + ") does not match roster student count (" + rosterStudents.size() + ")"));
        }

        // Clean up any existing unsubmitted records for this session
        attendanceRepo.deleteBySessionId(session.getId());

        int presentCount = 0;
        int absentCount = 0;
        List<AttendanceRecord> recordsToSave = new ArrayList<>();
        Instant now = Instant.now();

        for (StudentAttendanceItem item : req.records()) {
            Student student = rosterStudents.get(item.studentId());
            String status = item.status().toUpperCase();
            if ("PRESENT".equals(status)) {
                presentCount++;
            } else {
                absentCount++;
            }
            AttendanceRecord record = new AttendanceRecord();
            record.setSession(session);
            record.setStudent(student);
            record.setStatus(status);
            record.setMarkedAt(now);
            recordsToSave.add(record);
        }

        attendanceRepo.saveAll(recordsToSave);

        // Update session status to SUBMITTED
        session.setStatus("SUBMITTED");
        session.setSubmittedAt(now);
        sessionRepo.save(session);

        return ResponseEntity.ok(new SubmitAttendanceResponse(
                session.getId(),
                session.getStatus(),
                session.getSubmittedAt(),
                recordsToSave.size(),
                presentCount,
                absentCount
        ));
    }

    /**
     * Get attendance records for a session.
     */
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<?> getSessionAttendance(@PathVariable Long sessionId, Authentication auth) {
        ClassSession session = sessionRepo.findById(sessionId).orElse(null);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            String username = auth.getName();
            Faculty faculty = facultyRepo.findByUserUsername(username).orElse(null);
            if (faculty == null || !session.getTimetableEntry().getFaculty().getId().equals(faculty.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorBody("You are not authorized to view attendance records for this session"));
            }
        }

        List<AttendanceRecord> records = attendanceRepo.findBySessionId(sessionId);
        List<AttendanceRecordDto> dtos = records.stream()
                .map(r -> new AttendanceRecordDto(
                        r.getId(),
                        r.getStudent().getId(),
                        r.getStudent().getRegistrationNumber(),
                        r.getStudent().getName(),
                        r.getStatus(),
                        r.getMarkedAt()
                ))
                .sorted(Comparator.comparing(AttendanceRecordDto::registrationNumber))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }
}
