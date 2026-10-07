package com.amcs.session;

import com.amcs.attendance.AttendanceRecord;
import com.amcs.attendance.AttendanceRecordRepository;
import com.amcs.enrollment.Enrollment;
import com.amcs.enrollment.EnrollmentRepository;
import com.amcs.faculty.Faculty;
import com.amcs.faculty.FacultyRepository;
import com.amcs.timetable.TimetableEntry;
import com.amcs.timetable.TimetableEntryRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/sessions")
public class ClassSessionController {

    private final ClassSessionRepository sessionRepo;
    private final TimetableEntryRepository timetableRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final FacultyRepository facultyRepo;
    private final AttendanceRecordRepository attendanceRepo;
    private final com.amcs.holiday.HolidayRepository holidayRepo;
    private final com.amcs.calendar.AcademicCalendarService calendarService;

    public ClassSessionController(ClassSessionRepository sessionRepo,
                                  TimetableEntryRepository timetableRepo,
                                  EnrollmentRepository enrollmentRepo,
                                  FacultyRepository facultyRepo,
                                  AttendanceRecordRepository attendanceRepo,
                                  com.amcs.holiday.HolidayRepository holidayRepo,
                                  com.amcs.calendar.AcademicCalendarService calendarService) {
        this.sessionRepo = sessionRepo;
        this.timetableRepo = timetableRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.facultyRepo = facultyRepo;
        this.attendanceRepo = attendanceRepo;
        this.holidayRepo = holidayRepo;
        this.calendarService = calendarService;
    }

    public record CreateSessionRequest(
            @NotNull Long timetableEntryId,
            @NotNull LocalDate sessionDate
    ) {}

    public record RosterStudent(
            Long studentId,
            String registrationNumber,
            String name,
            Long labBatchId,
            String labBatchName,
            String status
    ) {}

    public record SessionRosterResponse(
            Long sessionId,
            LocalDate sessionDate,
            String status,
            String subjectCode,
            String subjectName,
            String subjectType,
            String sectionName,
            Long labBatchId,
            String labBatchName,
            String facultyName,
            int totalStudents,
            List<RosterStudent> students
    ) {}

    public record ErrorBody(String message) {}

    public record TodayClassDto(
            TimetableEntry timetableEntry,
            ClassSession session
    ) {}

    private boolean isAuthorizedForSession(ClassSession session, Authentication auth) {
        if (auth == null) return false;
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return true;
        String username = auth.getName();
        Faculty faculty = facultyRepo.findByUserUsername(username).orElse(null);
        return faculty != null && session.getTimetableEntry().getFaculty().getId().equals(faculty.getId());
    }

    /**
     * Create or open a session for a timetable entry on a given date.
     * Idempotent: If a session already exists for (timetableEntry, date), returns it.
     */
    @PostMapping
    @Transactional
    public ResponseEntity<?> createOrOpenSession(@Valid @RequestBody CreateSessionRequest req,
                                                 Authentication auth) {
        TimetableEntry entry = timetableRepo.findById(req.timetableEntryId()).orElse(null);
        if (entry == null || !entry.isActive()) {
            return ResponseEntity.badRequest().body(new ErrorBody("Timetable entry not found or inactive"));
        }

        // Authorization check: Admin can open any session, Faculty can only open their own
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            String username = auth.getName();
            Faculty faculty = facultyRepo.findByUserUsername(username).orElse(null);
            if (faculty == null || !entry.getFaculty().getId().equals(faculty.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorBody("You are not authorized to open a session for this class"));
            }
        }

        // Business Rule 1: No future dates allowed
        LocalDate today = LocalDate.now();
        if (req.sessionDate().isAfter(today)) {
            return ResponseEntity.badRequest().body(new ErrorBody("Cannot create session for future date: " + req.sessionDate()));
        }

        // Business Rule 2: Date weekday must strictly match timetable scheduled day
        String scheduledDay = entry.getDayOfWeek().toUpperCase();
        String requestedDay = req.sessionDate().getDayOfWeek().name().toUpperCase();
        if (!scheduledDay.equals(requestedDay)) {
            return ResponseEntity.badRequest().body(new ErrorBody(
                    String.format("Session date %s is a %s, but timetable entry is scheduled for %s",
                            req.sessionDate(), requestedDay, scheduledDay)));
        }

        // Business Rule 3: Date cannot be a weekly holiday (Sunday), institutional holiday (2nd Saturday), or declared holiday
        var dayStatus = calendarService.getDayStatus(req.sessionDate());
        if (dayStatus.isNonWorkingDay()) {
            return ResponseEntity.badRequest().body(new ErrorBody(
                    String.format("Cannot open session: %s is a non-working day (%s)",
                            req.sessionDate(), dayStatus.reason())));
        }

        // Check if session already exists for this timetable entry and date
        var existing = sessionRepo.findByTimetableEntryIdAndSessionDate(entry.getId(), req.sessionDate());
        if (existing.isPresent()) {
            return ResponseEntity.ok(existing.get());
        }

        // Create new session
        ClassSession session = new ClassSession();
        session.setTimetableEntry(entry);
        session.setSessionDate(req.sessionDate());
        session.setStatus("OPEN");
        ClassSession saved = sessionRepo.save(session);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getSession(@PathVariable Long id, Authentication auth) {
        ClassSession session = sessionRepo.findById(id).orElse(null);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        if (!isAuthorizedForSession(session, auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorBody("You are not authorized to access this session"));
        }
        return ResponseEntity.ok(session);
    }

    /**
     * Retrieves the roster for a session:
     * - THEORY: all active students enrolled in the section
     * - LAB: only students in the session's specific lab batch
     * - Status defaults to PRESENT unless attendance has already been recorded
     * - Guaranteed each student appears exactly once
     */
    @GetMapping("/{id}/roster")
    public ResponseEntity<?> getSessionRoster(@PathVariable Long id, Authentication auth) {
        ClassSession session = sessionRepo.findById(id).orElse(null);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        if (!isAuthorizedForSession(session, auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorBody("You are not authorized to access the roster for this class session"));
        }

        TimetableEntry entry = session.getTimetableEntry();
        Long sectionId = entry.getSection().getId();

        List<Enrollment> enrollments;
        if (entry.getLabBatch() != null) {
            // Lab session: only students in this specific lab batch
            enrollments = enrollmentRepo.findBySectionIdAndLabBatchId(sectionId, entry.getLabBatch().getId());
        } else {
            // Theory session: all students enrolled in section
            enrollments = enrollmentRepo.findBySectionIdOrdered(sectionId);
        }

        // Fetch existing attendance records if any
        List<AttendanceRecord> records = attendanceRepo.findBySessionId(session.getId());
        Map<Long, String> statusMap = records.stream()
                .collect(Collectors.toMap(r -> r.getStudent().getId(), AttendanceRecord::getStatus, (s1, s2) -> s1));

        // Deduplicate students by student ID while preserving order
        Map<Long, RosterStudent> studentMap = new LinkedHashMap<>();
        for (Enrollment e : enrollments) {
            var student = e.getStudent();
            if (student == null || !student.isActive()) continue;
            if (!studentMap.containsKey(student.getId())) {
                String status = statusMap.getOrDefault(student.getId(), "PRESENT");
                studentMap.put(student.getId(), new RosterStudent(
                        student.getId(),
                        student.getRegistrationNumber(),
                        student.getName(),
                        e.getLabBatch() != null ? e.getLabBatch().getId() : null,
                        e.getLabBatch() != null ? e.getLabBatch().getName() : null,
                        status
                ));
            }
        }

        List<RosterStudent> rosterList = new ArrayList<>(studentMap.values());

        SessionRosterResponse resp = new SessionRosterResponse(
                session.getId(),
                session.getSessionDate(),
                session.getStatus(),
                entry.getSubject().getCode(),
                entry.getSubject().getName(),
                entry.getSubject().getType(),
                entry.getSection().getName(),
                entry.getLabBatch() != null ? entry.getLabBatch().getId() : null,
                entry.getLabBatch() != null ? entry.getLabBatch().getName() : null,
                entry.getFaculty().getUser().getFullName(),
                rosterList.size(),
                rosterList
        );

        return ResponseEntity.ok(resp);
    }

    @GetMapping
    public List<ClassSession> listSessions(@RequestParam(required = false) Long sectionId,
                                           @RequestParam(required = false) Long facultyId,
                                           @RequestParam(required = false) LocalDate date) {
        if (facultyId != null && date != null) {
            return sessionRepo.findByFacultyIdAndSessionDate(facultyId, date);
        }
        if (sectionId != null) {
            return sessionRepo.findBySectionId(sectionId);
        }
        if (facultyId != null) {
            return sessionRepo.findByFacultyId(facultyId);
        }
        if (date != null) {
            return sessionRepo.findBySessionDate(date);
        }
        return sessionRepo.findAll();
    }

    /**
     * Today's sessions for authenticated faculty with corresponding timetable entries.
     */
    @GetMapping("/today")
    public ResponseEntity<?> getTodaySessions(@AuthenticationPrincipal String username) {
        var faculty = facultyRepo.findByUserUsername(username).orElse(null);
        if (faculty == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorBody("Not a faculty account"));
        }

        LocalDate today = LocalDate.now();
        String dayName = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toUpperCase();

        List<TimetableEntry> entries = timetableRepo.findByFacultyAndDay(faculty.getId(), dayName);
        List<ClassSession> existingSessions = sessionRepo.findByFacultyIdAndSessionDate(faculty.getId(), today);
        Map<Long, ClassSession> sessionByEntryId = existingSessions.stream()
                .collect(Collectors.toMap(s -> s.getTimetableEntry().getId(), s -> s, (s1, s2) -> s1));

        List<TodayClassDto> result = entries.stream()
                .map(e -> new TodayClassDto(e, sessionByEntryId.get(e.getId())))
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * All sessions for authenticated faculty.
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMySessions(@AuthenticationPrincipal String username) {
        var faculty = facultyRepo.findByUserUsername(username).orElse(null);
        if (faculty == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorBody("Not a faculty account"));
        }
        return ResponseEntity.ok(sessionRepo.findByFacultyId(faculty.getId()));
    }
}
