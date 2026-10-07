package com.amcs.attendance;

import com.amcs.enrollment.Enrollment;
import com.amcs.enrollment.EnrollmentRepository;
import com.amcs.section.Section;
import com.amcs.section.SectionRepository;
import com.amcs.session.ClassSession;
import com.amcs.session.ClassSessionRepository;
import com.amcs.student.Student;
import com.amcs.subject.Subject;
import com.amcs.timetable.TimetableEntry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceSummaryController {

    private final AttendanceRecordRepository attendanceRepo;
    private final ClassSessionRepository sessionRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final SectionRepository sectionRepo;

    public AttendanceSummaryController(AttendanceRecordRepository attendanceRepo,
                                       ClassSessionRepository sessionRepo,
                                       EnrollmentRepository enrollmentRepo,
                                       SectionRepository sectionRepo) {
        this.attendanceRepo = attendanceRepo;
        this.sessionRepo = sessionRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.sectionRepo = sectionRepo;
    }

    // ==========================================
    // REGISTER DTOs
    // ==========================================
    public record RegisterSessionColumn(
            Long id,
            LocalDate date,
            String dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            String subjectCode,
            String subjectName,
            String subjectType,
            Long labBatchId,
            String labBatchName,
            String room
    ) {}

    public record RegisterStudentRow(
            Long studentId,
            String registrationNumber,
            String name,
            Long labBatchId,
            String labBatchName,
            Map<Long, String> attendance, // sessionId -> "P", "A", or "—"
            int presentCount,
            int absentCount,
            int totalEligible,
            double percentage
    ) {}

    public record RegisterResponse(
            Long sectionId,
            String sectionName,
            Integer month,
            Integer year,
            int totalSessions,
            List<RegisterSessionColumn> sessions,
            List<RegisterStudentRow> students
    ) {}

    // ==========================================
    // MONTHLY SUMMARY DTOs
    // ==========================================
    public record SubjectAttendanceStat(
            String subjectCode,
            String subjectName,
            String subjectType,
            int present,
            int absent,
            int total,
            double percentage
    ) {}

    public record StudentMonthlySummary(
            Long studentId,
            String registrationNumber,
            String name,
            String labBatchName,
            Map<String, SubjectAttendanceStat> subjects,
            int totalPresent,
            int totalEligible,
            double overallPercentage
    ) {}

    public record MonthlySummaryResponse(
            Long sectionId,
            String sectionName,
            int month,
            int year,
            int totalSubmittedSessions,
            List<String> subjectCodes,
            List<StudentMonthlySummary> students,
            double sectionAveragePercentage
    ) {}

    // ==========================================
    // SUBJECT-WISE SUMMARY DTOs
    // ==========================================
    public record SubjectComponentStat(
            int present,
            int total,
            double percentage
    ) {}

    public record SubjectWiseDetail(
            String subjectCode,
            String subjectName,
            SubjectComponentStat theory,
            SubjectComponentStat lab,
            SubjectComponentStat combined
    ) {}

    public record StudentSubjectWiseSummary(
            Long studentId,
            String registrationNumber,
            String name,
            String labBatchName,
            Map<String, SubjectWiseDetail> subjects,
            int totalPresent,
            int totalEligible,
            double overallPercentage
    ) {}

    public record SubjectWiseSummaryResponse(
            Long sectionId,
            String sectionName,
            List<String> baseSubjectCodes,
            List<StudentSubjectWiseSummary> students,
            double sectionAveragePercentage
    ) {}

    // ==========================================
    // OVERALL SUMMARY DTOs
    // ==========================================
    public record StudentOverallSummary(
            Long studentId,
            String registrationNumber,
            String name,
            String labBatchName,
            int attendedClasses,
            int absentClasses,
            int totalClasses,
            double percentage,
            String status // GOOD, WARNING, CRITICAL
    ) {}

    public record OverallSummaryResponse(
            Long sectionId,
            String sectionName,
            int totalStudents,
            int totalSubmittedSessions,
            double averageAttendancePercentage,
            int goodCount,
            int warningCount,
            int criticalCount,
            List<StudentOverallSummary> students
    ) {}

    /**
     * 1. Detailed Attendance Register (Date x Session Grid)
     */
    @GetMapping("/register")
    public ResponseEntity<?> getDetailedRegister(
            @RequestParam Long sectionId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {

        Section section = sectionRepo.findById(sectionId).orElse(null);
        if (section == null) {
            return ResponseEntity.notFound().build();
        }

        // Fetch students ordered by registration number
        List<Enrollment> enrollments = enrollmentRepo.findBySectionIdOrdered(sectionId);

        // Fetch submitted sessions
        List<ClassSession> sessions;
        if (month != null && year != null) {
            LocalDate start = LocalDate.of(year, month, 1);
            LocalDate end = start.plusMonths(1).minusDays(1);
            sessions = sessionRepo.findSubmittedBySectionAndDateRange(sectionId, start, end);
        } else {
            sessions = sessionRepo.findSubmittedBySection(sectionId);
        }

        List<RegisterSessionColumn> sessionCols = sessions.stream().map(s -> {
            TimetableEntry te = s.getTimetableEntry();
            return new RegisterSessionColumn(
                    s.getId(),
                    s.getSessionDate(),
                    te.getDayOfWeek(),
                    te.getStartTime(),
                    te.getEndTime(),
                    te.getSubject().getCode(),
                    te.getSubject().getName(),
                    te.getSubject().getType(),
                    te.getLabBatch() != null ? te.getLabBatch().getId() : null,
                    te.getLabBatch() != null ? te.getLabBatch().getName() : null,
                    te.getRoom()
            );
        }).collect(Collectors.toList());

        // Batch load attendance records
        List<Long> sessionIds = sessions.stream().map(ClassSession::getId).toList();
        Map<String, String> recordMap = new HashMap<>();
        if (!sessionIds.isEmpty()) {
            List<AttendanceRecord> records = attendanceRepo.findBySessionIdIn(sessionIds);
            for (AttendanceRecord r : records) {
                recordMap.put(r.getSession().getId() + "_" + r.getStudent().getId(), r.getStatus());
            }
        }

        // Build student rows
        List<RegisterStudentRow> studentRows = new ArrayList<>();
        for (Enrollment e : enrollments) {
            Student student = e.getStudent();
            if (student == null || !student.isActive()) continue;

            Long studentLabBatchId = e.getLabBatch() != null ? e.getLabBatch().getId() : null;
            String studentLabBatchName = e.getLabBatch() != null ? e.getLabBatch().getName() : null;

            Map<Long, String> attMap = new HashMap<>();
            int present = 0;
            int absent = 0;
            int eligible = 0;

            for (ClassSession s : sessions) {
                TimetableEntry te = s.getTimetableEntry();
                Long sessionLabBatchId = te.getLabBatch() != null ? te.getLabBatch().getId() : null;

                if (sessionLabBatchId != null) {
                    // Lab session: check if student belongs to this batch
                    if (studentLabBatchId == null || !studentLabBatchId.equals(sessionLabBatchId)) {
                        attMap.put(s.getId(), "—"); // Em dash: Not applicable
                        continue;
                    }
                }

                // Student is eligible for this session
                eligible++;
                String key = s.getId() + "_" + student.getId();
                String status = recordMap.get(key);
                if ("PRESENT".equalsIgnoreCase(status)) {
                    present++;
                    attMap.put(s.getId(), "P");
                } else if ("ABSENT".equalsIgnoreCase(status)) {
                    absent++;
                    attMap.put(s.getId(), "A");
                } else {
                    attMap.put(s.getId(), "");
                }
            }

            double pct = eligible > 0 ? Math.round((present * 1000.0) / eligible) / 10.0 : 0.0;
            studentRows.add(new RegisterStudentRow(
                    student.getId(),
                    student.getRegistrationNumber(),
                    student.getName(),
                    studentLabBatchId,
                    studentLabBatchName,
                    attMap,
                    present,
                    absent,
                    eligible,
                    pct
            ));
        }

        return ResponseEntity.ok(new RegisterResponse(
                sectionId,
                section.getName(),
                month,
                year,
                sessionCols.size(),
                sessionCols,
                studentRows
        ));
    }

    /**
     * 2. Monthly Summary
     */
    @GetMapping("/summary/monthly")
    public ResponseEntity<?> getMonthlySummary(
            @RequestParam Long sectionId,
            @RequestParam int month,
            @RequestParam int year) {

        Section section = sectionRepo.findById(sectionId).orElse(null);
        if (section == null) return ResponseEntity.notFound().build();

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        List<ClassSession> sessions = sessionRepo.findSubmittedBySectionAndDateRange(sectionId, start, end);
        List<Enrollment> enrollments = enrollmentRepo.findBySectionIdOrdered(sectionId);

        // All distinct subject codes taught this month
        List<String> subjectCodes = sessions.stream()
                .map(s -> s.getTimetableEntry().getSubject().getCode())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        List<Long> sessionIds = sessions.stream().map(ClassSession::getId).toList();
        Map<String, String> recordMap = new HashMap<>();
        if (!sessionIds.isEmpty()) {
            for (AttendanceRecord r : attendanceRepo.findBySessionIdIn(sessionIds)) {
                recordMap.put(r.getSession().getId() + "_" + r.getStudent().getId(), r.getStatus());
            }
        }

        List<StudentMonthlySummary> studentSummaries = new ArrayList<>();
        double totalPctSum = 0;

        for (Enrollment e : enrollments) {
            Student student = e.getStudent();
            if (student == null || !student.isActive()) continue;

            Long studentLabBatchId = e.getLabBatch() != null ? e.getLabBatch().getId() : null;
            Map<String, SubjectAttendanceStat> subStats = new LinkedHashMap<>();

            int studentTotalPresent = 0;
            int studentTotalEligible = 0;

            for (String subCode : subjectCodes) {
                List<ClassSession> subSessions = sessions.stream()
                        .filter(s -> s.getTimetableEntry().getSubject().getCode().equals(subCode))
                        .toList();

                Subject subject = subSessions.get(0).getTimetableEntry().getSubject();

                int p = 0;
                int a = 0;
                int el = 0;

                for (ClassSession s : subSessions) {
                    TimetableEntry te = s.getTimetableEntry();
                    Long batchId = te.getLabBatch() != null ? te.getLabBatch().getId() : null;
                    if (batchId != null && (studentLabBatchId == null || !studentLabBatchId.equals(batchId))) {
                        continue; // Student not in this lab batch
                    }
                    el++;
                    String status = recordMap.get(s.getId() + "_" + student.getId());
                    if ("PRESENT".equalsIgnoreCase(status)) p++;
                    else if ("ABSENT".equalsIgnoreCase(status)) a++;
                }

                double subPct = el > 0 ? Math.round((p * 1000.0) / el) / 10.0 : 0.0;
                subStats.put(subCode, new SubjectAttendanceStat(
                        subCode,
                        subject.getName(),
                        subject.getType(),
                        p, a, el, subPct
                ));

                studentTotalPresent += p;
                studentTotalEligible += el;
            }

            double overallPct = studentTotalEligible > 0 ? Math.round((studentTotalPresent * 1000.0) / studentTotalEligible) / 10.0 : 0.0;
            totalPctSum += overallPct;

            studentSummaries.add(new StudentMonthlySummary(
                    student.getId(),
                    student.getRegistrationNumber(),
                    student.getName(),
                    e.getLabBatch() != null ? e.getLabBatch().getName() : null,
                    subStats,
                    studentTotalPresent,
                    studentTotalEligible,
                    overallPct
            ));
        }

        double avgPct = studentSummaries.isEmpty() ? 0.0 : Math.round((totalPctSum / studentSummaries.size()) * 10.0) / 10.0;

        return ResponseEntity.ok(new MonthlySummaryResponse(
                sectionId,
                section.getName(),
                month,
                year,
                sessions.size(),
                subjectCodes,
                studentSummaries,
                avgPct
        ));
    }

    /**
     * 3. Subject-wise Summary (Theory + Lab components)
     */
    @GetMapping("/summary/subject-wise")
    public ResponseEntity<?> getSubjectWiseSummary(@RequestParam Long sectionId) {
        Section section = sectionRepo.findById(sectionId).orElse(null);
        if (section == null) return ResponseEntity.notFound().build();

        List<ClassSession> sessions = sessionRepo.findSubmittedBySection(sectionId);
        List<Enrollment> enrollments = enrollmentRepo.findBySectionIdOrdered(sectionId);

        // Find unique base subjects (e.g. OS, OOPJ, COA, DMS, PSQT)
        Set<String> baseSubjects = new LinkedHashSet<>();
        for (ClassSession s : sessions) {
            String code = s.getTimetableEntry().getSubject().getCode();
            String base = code.replace("-LAB", "");
            baseSubjects.add(base);
        }

        List<Long> sessionIds = sessions.stream().map(ClassSession::getId).toList();
        Map<String, String> recordMap = new HashMap<>();
        if (!sessionIds.isEmpty()) {
            for (AttendanceRecord r : attendanceRepo.findBySessionIdIn(sessionIds)) {
                recordMap.put(r.getSession().getId() + "_" + r.getStudent().getId(), r.getStatus());
            }
        }

        List<StudentSubjectWiseSummary> studentSummaries = new ArrayList<>();
        double totalPctSum = 0;

        for (Enrollment e : enrollments) {
            Student student = e.getStudent();
            if (student == null || !student.isActive()) continue;

            Long studentLabBatchId = e.getLabBatch() != null ? e.getLabBatch().getId() : null;
            Map<String, SubjectWiseDetail> subDetails = new LinkedHashMap<>();

            int studentTotalPresent = 0;
            int studentTotalEligible = 0;

            for (String base : baseSubjects) {
                // Theory sessions for base
                List<ClassSession> theorySessions = sessions.stream()
                        .filter(s -> s.getTimetableEntry().getSubject().getCode().equals(base))
                        .toList();
                // Lab sessions for base (base + "-LAB")
                List<ClassSession> labSessions = sessions.stream()
                        .filter(s -> s.getTimetableEntry().getSubject().getCode().equals(base + "-LAB"))
                        .toList();

                int thP = 0, thEl = 0;
                for (ClassSession s : theorySessions) {
                    thEl++;
                    if ("PRESENT".equalsIgnoreCase(recordMap.get(s.getId() + "_" + student.getId()))) thP++;
                }
                double thPct = thEl > 0 ? Math.round((thP * 1000.0) / thEl) / 10.0 : 0.0;

                int lbP = 0, lbEl = 0;
                for (ClassSession s : labSessions) {
                    Long batchId = s.getTimetableEntry().getLabBatch() != null ? s.getTimetableEntry().getLabBatch().getId() : null;
                    if (batchId != null && (studentLabBatchId == null || !studentLabBatchId.equals(batchId))) {
                        continue;
                    }
                    lbEl++;
                    if ("PRESENT".equalsIgnoreCase(recordMap.get(s.getId() + "_" + student.getId()))) lbP++;
                }
                double lbPct = lbEl > 0 ? Math.round((lbP * 1000.0) / lbEl) / 10.0 : 0.0;

                int combP = thP + lbP;
                int combEl = thEl + lbEl;
                double combPct = combEl > 0 ? Math.round((combP * 1000.0) / combEl) / 10.0 : 0.0;

                subDetails.put(base, new SubjectWiseDetail(
                        base,
                        base,
                        new SubjectComponentStat(thP, thEl, thPct),
                        new SubjectComponentStat(lbP, lbEl, lbPct),
                        new SubjectComponentStat(combP, combEl, combPct)
                ));

                studentTotalPresent += combP;
                studentTotalEligible += combEl;
            }

            double overallPct = studentTotalEligible > 0 ? Math.round((studentTotalPresent * 1000.0) / studentTotalEligible) / 10.0 : 0.0;
            totalPctSum += overallPct;

            studentSummaries.add(new StudentSubjectWiseSummary(
                    student.getId(),
                    student.getRegistrationNumber(),
                    student.getName(),
                    e.getLabBatch() != null ? e.getLabBatch().getName() : null,
                    subDetails,
                    studentTotalPresent,
                    studentTotalEligible,
                    overallPct
            ));
        }

        double avgPct = studentSummaries.isEmpty() ? 0.0 : Math.round((totalPctSum / studentSummaries.size()) * 10.0) / 10.0;

        return ResponseEntity.ok(new SubjectWiseSummaryResponse(
                sectionId,
                section.getName(),
                new ArrayList<>(baseSubjects),
                studentSummaries,
                avgPct
        ));
    }

    /**
     * 4. Overall Summary
     */
    @GetMapping("/summary/overall")
    public ResponseEntity<?> getOverallSummary(@RequestParam Long sectionId) {
        Section section = sectionRepo.findById(sectionId).orElse(null);
        if (section == null) return ResponseEntity.notFound().build();

        List<ClassSession> sessions = sessionRepo.findSubmittedBySection(sectionId);
        List<Enrollment> enrollments = enrollmentRepo.findBySectionIdOrdered(sectionId);

        List<Long> sessionIds = sessions.stream().map(ClassSession::getId).toList();
        Map<String, String> recordMap = new HashMap<>();
        if (!sessionIds.isEmpty()) {
            for (AttendanceRecord r : attendanceRepo.findBySessionIdIn(sessionIds)) {
                recordMap.put(r.getSession().getId() + "_" + r.getStudent().getId(), r.getStatus());
            }
        }

        List<StudentOverallSummary> studentList = new ArrayList<>();
        int good = 0;
        int warning = 0;
        int critical = 0;
        double totalPctSum = 0;

        for (Enrollment e : enrollments) {
            Student student = e.getStudent();
            if (student == null || !student.isActive()) continue;

            Long studentLabBatchId = e.getLabBatch() != null ? e.getLabBatch().getId() : null;

            int p = 0;
            int a = 0;
            int el = 0;

            for (ClassSession s : sessions) {
                Long batchId = s.getTimetableEntry().getLabBatch() != null ? s.getTimetableEntry().getLabBatch().getId() : null;
                if (batchId != null && (studentLabBatchId == null || !studentLabBatchId.equals(batchId))) {
                    continue;
                }
                el++;
                String status = recordMap.get(s.getId() + "_" + student.getId());
                if ("PRESENT".equalsIgnoreCase(status)) p++;
                else if ("ABSENT".equalsIgnoreCase(status)) a++;
            }

            double pct = el > 0 ? Math.round((p * 1000.0) / el) / 10.0 : 0.0;
            totalPctSum += pct;

            String statusLevel;
            if (pct >= 75.0) {
                statusLevel = "GOOD";
                good++;
            } else if (pct >= 65.0) {
                statusLevel = "WARNING";
                warning++;
            } else {
                statusLevel = "CRITICAL";
                critical++;
            }

            studentList.add(new StudentOverallSummary(
                    student.getId(),
                    student.getRegistrationNumber(),
                    student.getName(),
                    e.getLabBatch() != null ? e.getLabBatch().getName() : null,
                    p, a, el, pct, statusLevel
            ));
        }

        double avgPct = studentList.isEmpty() ? 0.0 : Math.round((totalPctSum / studentList.size()) * 10.0) / 10.0;

        return ResponseEntity.ok(new OverallSummaryResponse(
                sectionId,
                section.getName(),
                studentList.size(),
                sessions.size(),
                avgPct,
                good,
                warning,
                critical,
                studentList
        ));
    }
}
