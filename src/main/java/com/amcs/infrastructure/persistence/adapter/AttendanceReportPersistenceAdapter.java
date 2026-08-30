package com.amcs.infrastructure.persistence.adapter;

import com.amcs.application.dto.report.AttendanceRegisterReportData;
import com.amcs.application.dto.report.CondonationRegisterReportRow;
import com.amcs.application.dto.report.DefaulterReportRow;
import com.amcs.application.dto.report.FacultyComplianceReportRow;
import com.amcs.application.dto.report.OverallAttendanceReportRow;
import com.amcs.application.dto.report.PredictionReportRow;
import com.amcs.application.dto.report.StudentAttendanceReportRow;
import com.amcs.application.dto.report.SubjectAttendanceSummaryReportRow;
import com.amcs.application.port.out.report.AttendanceReportDataPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class AttendanceReportPersistenceAdapter implements AttendanceReportDataPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public List<StudentAttendanceReportRow> getStudentAttendanceReport(
        UUID studentId, UUID academicPeriodId, UUID sectionId
    ) {
        String sql = """
            SELECT sub.id AS sub_id, sub.code AS sub_code, sub.name AS sub_name, sub.course_type,
                   COALESCE(SUM(s.conducted_units), 0) AS conducted_units,
                   COALESCE(SUM(CASE WHEN ar.status IN ('PRESENT', 'LATE', 'DUTY_LEAVE', 'MEDICAL_LEAVE')
                                     THEN s.conducted_units ELSE 0 END), 0) AS attended_units
            FROM subjects sub
            JOIN sessions s ON s.subject_id = sub.id AND s.academic_period_id = :periodId AND s.status = 'CONDUCTED'
            LEFT JOIN attendance_records ar ON ar.session_id = s.id AND ar.student_id = :studentId
            WHERE (CAST(:sectionId AS uuid) IS NULL OR s.section_id = :sectionId)
            GROUP BY sub.id, sub.code, sub.name, sub.course_type
            ORDER BY sub.code
        """;

        Query query = entityManager.createNativeQuery(sql)
            .setParameter("periodId", academicPeriodId)
            .setParameter("studentId", studentId)
            .setParameter("sectionId", sectionId);

        List<Object[]> results = query.getResultList();
        List<StudentAttendanceReportRow> rows = new ArrayList<>(results.size());

        for (Object[] r : results) {
            UUID subId = (UUID) r[0];
            String code = (String) r[1];
            String name = (String) r[2];
            String courseType = (String) r[3];
            int conducted = ((Number) r[4]).intValue();
            int attended = ((Number) r[5]).intValue();

            BigDecimal percentage = calculatePercentage(conducted, attended);
            String status = determineStatus(percentage, new BigDecimal("75.00"), new BigDecimal("65.00"));

            rows.add(new StudentAttendanceReportRow(
                subId, code, name, courseType, conducted, attended, percentage, status
            ));
        }
        return rows;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SubjectAttendanceSummaryReportRow> getSubjectAttendanceSummary(
        UUID subjectId, UUID sectionId, UUID academicPeriodId
    ) {
        String sql = """
            SELECT st.id AS student_id, st.registration_number, st.name AS student_name, d.name AS dept_name,
                   (SELECT COALESCE(SUM(s2.conducted_units), 0)
                    FROM sessions s2
                    WHERE s2.subject_id = :subjectId AND s2.section_id = :sectionId
                      AND s2.academic_period_id = :periodId AND s2.status = 'CONDUCTED') AS conducted_units,
                   COALESCE(SUM(CASE WHEN ar.status IN ('PRESENT', 'LATE', 'DUTY_LEAVE', 'MEDICAL_LEAVE')
                                     THEN s.conducted_units ELSE 0 END), 0) AS attended_units
            FROM enrollments e
            JOIN students st ON st.id = e.student_id
            JOIN departments d ON d.id = st.department_id
            LEFT JOIN sessions s ON s.subject_id = :subjectId AND s.section_id = :sectionId
                                AND s.academic_period_id = :periodId AND s.status = 'CONDUCTED'
            LEFT JOIN attendance_records ar ON ar.session_id = s.id AND ar.student_id = st.id
            WHERE e.section_id = :sectionId AND e.status = 'ACTIVE'
            GROUP BY st.id, st.registration_number, st.name, d.name
            ORDER BY st.registration_number
        """;

        Query query = entityManager.createNativeQuery(sql)
            .setParameter("subjectId", subjectId)
            .setParameter("sectionId", sectionId)
            .setParameter("periodId", academicPeriodId);

        List<Object[]> results = query.getResultList();
        List<SubjectAttendanceSummaryReportRow> rows = new ArrayList<>(results.size());

        for (Object[] r : results) {
            UUID sId = (UUID) r[0];
            String regNo = (String) r[1];
            String name = (String) r[2];
            String dept = (String) r[3];
            int conducted = ((Number) r[4]).intValue();
            int attended = ((Number) r[5]).intValue();

            BigDecimal percentage = calculatePercentage(conducted, attended);
            String status = determineStatus(percentage, new BigDecimal("75.00"), new BigDecimal("65.00"));

            rows.add(new SubjectAttendanceSummaryReportRow(
                sId, regNo, name, dept, conducted, attended, percentage, status
            ));
        }
        return rows;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DefaulterReportRow> getDefaulterReport(
        UUID academicPeriodId, UUID sectionId, UUID subjectId, BigDecimal threshold
    ) {
        String sql = """
            SELECT st.id AS student_id, st.registration_number, st.name AS student_name,
                   sub.code AS sub_code, sub.name AS sub_name, sec.name AS sec_name,
                   COALESCE(SUM(s.conducted_units), 0) AS conducted_units,
                   COALESCE(SUM(CASE WHEN ar.status IN ('PRESENT', 'LATE', 'DUTY_LEAVE', 'MEDICAL_LEAVE')
                                     THEN s.conducted_units ELSE 0 END), 0) AS attended_units
            FROM enrollments e
            JOIN students st ON st.id = e.student_id
            JOIN sections sec ON sec.id = e.section_id
            JOIN sessions s ON s.section_id = sec.id AND s.academic_period_id = :periodId AND s.status = 'CONDUCTED'
            JOIN subjects sub ON sub.id = s.subject_id
            LEFT JOIN attendance_records ar ON ar.session_id = s.id AND ar.student_id = st.id
            WHERE e.status = 'ACTIVE'
              AND (CAST(:sectionId AS uuid) IS NULL OR sec.id = :sectionId)
              AND (CAST(:subjectId AS uuid) IS NULL OR sub.id = :subjectId)
            GROUP BY st.id, st.registration_number, st.name, sub.code, sub.name, sec.name
            ORDER BY sec.name, sub.code, st.registration_number
        """;

        Query query = entityManager.createNativeQuery(sql)
            .setParameter("periodId", academicPeriodId)
            .setParameter("sectionId", sectionId)
            .setParameter("subjectId", subjectId);

        List<Object[]> results = query.getResultList();
        List<DefaulterReportRow> rows = new ArrayList<>();
        BigDecimal effThreshold = threshold != null ? threshold : new BigDecimal("75.00");

        for (Object[] r : results) {
            UUID sId = (UUID) r[0];
            String regNo = (String) r[1];
            String sName = (String) r[2];
            String subCode = (String) r[3];
            String subName = (String) r[4];
            String secName = (String) r[5];
            int conducted = ((Number) r[6]).intValue();
            int attended = ((Number) r[7]).intValue();

            BigDecimal percentage = calculatePercentage(conducted, attended);

            if (conducted > 0 && percentage.compareTo(effThreshold) < 0) {
                // Calculate units short: ceil((threshold * conducted - 100 * attended) / 100)
                BigDecimal requiredUnits = effThreshold.multiply(BigDecimal.valueOf(conducted))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.CEILING);
                int unitsShort = Math.max(0, requiredUnits.subtract(BigDecimal.valueOf(attended)).setScale(0, RoundingMode.CEILING).intValue());

                // Calculate classes required: ceil((threshold * conducted - 100 * attended) / (100 - threshold))
                int classesRequired;
                BigDecimal diff = BigDecimal.valueOf(100).subtract(effThreshold);
                if (diff.compareTo(BigDecimal.ZERO) <= 0) {
                    classesRequired = Integer.MAX_VALUE;
                } else {
                    BigDecimal num = effThreshold.multiply(BigDecimal.valueOf(conducted))
                        .subtract(BigDecimal.valueOf(100).multiply(BigDecimal.valueOf(attended)));
                    classesRequired = Math.max(0, num.divide(diff, 0, RoundingMode.CEILING).intValue());
                }

                rows.add(new DefaulterReportRow(
                    sId, regNo, sName, subCode, subName, secName, percentage, effThreshold, unitsShort, classesRequired
                ));
            }
        }
        return rows;
    }

    @Override
    @SuppressWarnings("unchecked")
    public AttendanceRegisterReportData getAttendanceRegisterReport(
        UUID subjectId, UUID sectionId, UUID academicPeriodId, LocalDate startDate, LocalDate endDate
    ) {
        // 1. Fetch Subject and Section names
        String metaSql = """
            SELECT sub.code, sub.name, sec.name
            FROM subjects sub, sections sec
            WHERE sub.id = :subjectId AND sec.id = :sectionId
        """;
        Object[] meta = (Object[]) entityManager.createNativeQuery(metaSql)
            .setParameter("subjectId", subjectId)
            .setParameter("sectionId", sectionId)
            .getSingleResult();

        String subCode = (String) meta[0];
        String subName = (String) meta[1];
        String secName = (String) meta[2];

        // 2. Fetch distinct session dates in chronological order
        String dateSql = """
            SELECT DISTINCT s.session_date
            FROM sessions s
            WHERE s.subject_id = :subjectId AND s.section_id = :sectionId
              AND s.academic_period_id = :periodId AND s.status = 'CONDUCTED'
              AND (CAST(:startDate AS date) IS NULL OR s.session_date >= :startDate)
              AND (CAST(:endDate AS date) IS NULL OR s.session_date <= :endDate)
            ORDER BY s.session_date ASC
        """;

        Query dateQuery = entityManager.createNativeQuery(dateSql)
            .setParameter("subjectId", subjectId)
            .setParameter("sectionId", sectionId)
            .setParameter("periodId", academicPeriodId)
            .setParameter("startDate", startDate != null ? Date.valueOf(startDate) : null)
            .setParameter("endDate", endDate != null ? Date.valueOf(endDate) : null);

        List<Date> dateResults = dateQuery.getResultList();
        List<LocalDate> sessionDates = dateResults.stream().map(Date::toLocalDate).toList();

        // 3. Fetch active enrolled students
        String studentSql = """
            SELECT st.id, st.registration_number, st.name
            FROM enrollments e
            JOIN students st ON st.id = e.student_id
            WHERE e.section_id = :sectionId AND e.status = 'ACTIVE'
            ORDER BY st.registration_number ASC
        """;

        List<Object[]> studentResults = entityManager.createNativeQuery(studentSql)
            .setParameter("sectionId", sectionId)
            .getResultList();

        // 4. Fetch all attendance records for this grid
        String recordSql = """
            SELECT ar.student_id, s.session_date, ar.status, s.conducted_units
            FROM attendance_records ar
            JOIN sessions s ON s.id = ar.session_id
            WHERE s.subject_id = :subjectId AND s.section_id = :sectionId
              AND s.academic_period_id = :periodId AND s.status = 'CONDUCTED'
              AND (CAST(:startDate AS date) IS NULL OR s.session_date >= :startDate)
              AND (CAST(:endDate AS date) IS NULL OR s.session_date <= :endDate)
        """;

        Query recordQuery = entityManager.createNativeQuery(recordSql)
            .setParameter("subjectId", subjectId)
            .setParameter("sectionId", sectionId)
            .setParameter("periodId", academicPeriodId)
            .setParameter("startDate", startDate != null ? Date.valueOf(startDate) : null)
            .setParameter("endDate", endDate != null ? Date.valueOf(endDate) : null);

        List<Object[]> recordResults = recordQuery.getResultList();
        Map<UUID, Map<LocalDate, String>> studentAttendanceMap = new HashMap<>();

        for (Object[] rec : recordResults) {
            UUID stId = (UUID) rec[0];
            LocalDate sDate = ((Date) rec[1]).toLocalDate();
            String stStatus = (String) rec[2];

            studentAttendanceMap.computeIfAbsent(stId, k -> new HashMap<>()).put(sDate, stStatus);
        }

        int totalConducted = sessionDates.size();
        List<AttendanceRegisterReportData.StudentRegisterRow> studentRows = new ArrayList<>(studentResults.size());

        for (Object[] st : studentResults) {
            UUID stId = (UUID) st[0];
            String regNo = (String) st[1];
            String name = (String) st[2];

            Map<LocalDate, String> attendanceByDate = studentAttendanceMap.getOrDefault(stId, Map.of());
            int totalAttended = 0;
            for (LocalDate d : sessionDates) {
                String code = attendanceByDate.get(d);
                if ("PRESENT".equalsIgnoreCase(code) || "LATE".equalsIgnoreCase(code)
                    || "DUTY_LEAVE".equalsIgnoreCase(code) || "MEDICAL_LEAVE".equalsIgnoreCase(code)) {
                    totalAttended++;
                }
            }

            BigDecimal percentage = calculatePercentage(totalConducted, totalAttended);

            studentRows.add(new AttendanceRegisterReportData.StudentRegisterRow(
                stId, regNo, name, attendanceByDate, totalConducted, totalAttended, percentage
            ));
        }

        return new AttendanceRegisterReportData(subCode, subName, secName, sessionDates, studentRows);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<OverallAttendanceReportRow> getOverallAttendanceSummary(
        UUID academicPeriodId, UUID sectionId, UUID departmentId, BigDecimal threshold
    ) {
        String sql = """
            SELECT st.id AS student_id, st.registration_number, st.name AS student_name, sec.name AS sec_name,
                   COUNT(DISTINCT s.subject_id) AS subjects_count,
                   COALESCE(SUM(s.conducted_units), 0) AS total_conducted,
                   COALESCE(SUM(CASE WHEN ar.status IN ('PRESENT', 'LATE', 'DUTY_LEAVE', 'MEDICAL_LEAVE')
                                     THEN s.conducted_units ELSE 0 END), 0) AS total_attended
            FROM enrollments e
            JOIN students st ON st.id = e.student_id
            JOIN sections sec ON sec.id = e.section_id
            LEFT JOIN sessions s ON s.section_id = sec.id AND s.academic_period_id = :periodId AND s.status = 'CONDUCTED'
            LEFT JOIN attendance_records ar ON ar.session_id = s.id AND ar.student_id = st.id
            WHERE e.status = 'ACTIVE'
              AND (CAST(:sectionId AS uuid) IS NULL OR sec.id = :sectionId)
              AND (CAST(:departmentId AS uuid) IS NULL OR sec.department_id = :departmentId)
            GROUP BY st.id, st.registration_number, st.name, sec.name
            ORDER BY sec.name, st.registration_number
        """;

        Query query = entityManager.createNativeQuery(sql)
            .setParameter("periodId", academicPeriodId)
            .setParameter("sectionId", sectionId)
            .setParameter("departmentId", departmentId);

        List<Object[]> results = query.getResultList();
        List<OverallAttendanceReportRow> rows = new ArrayList<>(results.size());
        BigDecimal effThreshold = threshold != null ? threshold : new BigDecimal("75.00");

        for (Object[] r : results) {
            UUID sId = (UUID) r[0];
            String regNo = (String) r[1];
            String name = (String) r[2];
            String secName = (String) r[3];
            int subjectsCount = ((Number) r[4]).intValue();
            int totalConducted = ((Number) r[5]).intValue();
            int totalAttended = ((Number) r[6]).intValue();

            BigDecimal percentage = calculatePercentage(totalConducted, totalAttended);
            String status = percentage.compareTo(effThreshold) >= 0 ? "ELIGIBLE" : "DEFAULTER";

            rows.add(new OverallAttendanceReportRow(
                sId, regNo, name, secName, subjectsCount, totalConducted, totalAttended, percentage, effThreshold, status
            ));
        }
        return rows;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<FacultyComplianceReportRow> getFacultyComplianceReport(
        UUID academicPeriodId, UUID facultyId, UUID departmentId
    ) {
        String sql = """
            SELECT f.id AS faculty_id, f.employee_id, f.name AS faculty_name, d.name AS dept_name,
                   sub.code AS sub_code, sub.name AS sub_name, sec.name AS sec_name,
                   COUNT(s.id) AS total_sessions,
                   COALESCE(SUM(CASE WHEN s.status = 'CONDUCTED' THEN 1 ELSE 0 END), 0) AS conducted_sessions,
                   COALESCE(SUM(CASE WHEN s.status <> 'CONDUCTED' THEN 1 ELSE 0 END), 0) AS unconducted_sessions
            FROM sessions s
            JOIN faculty f ON f.id = s.conducted_by_faculty_id
            JOIN departments d ON d.id = f.department_id
            JOIN subjects sub ON sub.id = s.subject_id
            JOIN sections sec ON sec.id = s.section_id
            WHERE s.academic_period_id = :periodId
              AND (CAST(:facultyId AS uuid) IS NULL OR f.id = :facultyId)
              AND (CAST(:departmentId AS uuid) IS NULL OR d.id = :departmentId)
            GROUP BY f.id, f.employee_id, f.name, d.name, sub.code, sub.name, sec.name
            ORDER BY f.name, sub.code, sec.name
        """;

        Query query = entityManager.createNativeQuery(sql)
            .setParameter("periodId", academicPeriodId)
            .setParameter("facultyId", facultyId)
            .setParameter("departmentId", departmentId);

        List<Object[]> results = query.getResultList();
        List<FacultyComplianceReportRow> rows = new ArrayList<>(results.size());

        for (Object[] r : results) {
            UUID fId = (UUID) r[0];
            String empId = (String) r[1];
            String fName = (String) r[2];
            String deptName = (String) r[3];
            String subCode = (String) r[4];
            String subName = (String) r[5];
            String secName = (String) r[6];
            int total = ((Number) r[7]).intValue();
            int conducted = ((Number) r[8]).intValue();
            int unconducted = ((Number) r[9]).intValue();

            BigDecimal compliance = calculatePercentage(total, conducted);

            rows.add(new FacultyComplianceReportRow(
                fId, empId, fName, deptName, subCode, subName, secName, total, conducted, unconducted, compliance
            ));
        }
        return rows;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CondonationRegisterReportRow> getCondonationRegisterReport(
        UUID academicPeriodId, UUID sectionId, UUID subjectId, String leaveType, LocalDate startDate, LocalDate endDate
    ) {
        String sql = """
            SELECT ar.id AS record_id, s.session_date, st.registration_number, st.name AS student_name,
                   sub.code AS sub_code, sub.name AS sub_name, sec.name AS sec_name,
                   f.name AS faculty_name, ar.status AS leave_type, ar.created_at
            FROM attendance_records ar
            JOIN sessions s ON s.id = ar.session_id
            JOIN students st ON st.id = ar.student_id
            JOIN subjects sub ON sub.id = s.subject_id
            JOIN sections sec ON sec.id = s.section_id
            JOIN faculty f ON f.id = s.conducted_by_faculty_id
            WHERE s.academic_period_id = :periodId
              AND ar.status IN ('DUTY_LEAVE', 'MEDICAL_LEAVE')
              AND (CAST(:sectionId AS uuid) IS NULL OR s.section_id = :sectionId)
              AND (CAST(:subjectId AS uuid) IS NULL OR s.subject_id = :subjectId)
              AND (CAST(:leaveType AS varchar) IS NULL OR ar.status = :leaveType)
              AND (CAST(:startDate AS date) IS NULL OR s.session_date >= :startDate)
              AND (CAST(:endDate AS date) IS NULL OR s.session_date <= :endDate)
            ORDER BY s.session_date DESC, st.registration_number
        """;

        Query query = entityManager.createNativeQuery(sql)
            .setParameter("periodId", academicPeriodId)
            .setParameter("sectionId", sectionId)
            .setParameter("subjectId", subjectId)
            .setParameter("leaveType", leaveType)
            .setParameter("startDate", startDate != null ? Date.valueOf(startDate) : null)
            .setParameter("endDate", endDate != null ? Date.valueOf(endDate) : null);

        List<Object[]> results = query.getResultList();
        List<CondonationRegisterReportRow> rows = new ArrayList<>(results.size());

        for (Object[] r : results) {
            UUID recId = (UUID) r[0];
            LocalDate sDate = ((Date) r[1]).toLocalDate();
            String regNo = (String) r[2];
            String stName = (String) r[3];
            String subCode = (String) r[4];
            String subName = (String) r[5];
            String secName = (String) r[6];
            String fName = (String) r[7];
            String lType = (String) r[8];
            Object ts = r[9];
            Instant recordedAt = ts instanceof Instant i ? i : (ts instanceof Timestamp t ? t.toInstant() : null);

            rows.add(new CondonationRegisterReportRow(
                recId, sDate, regNo, stName, subCode, subName, secName, fName, lType, recordedAt
            ));
        }
        return rows;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PredictionReportRow> getPredictionReport(
        UUID subjectId, UUID sectionId, UUID academicPeriodId, int projectedFutureSessions, BigDecimal targetThreshold
    ) {
        String sql = """
            SELECT st.id AS student_id, st.registration_number, st.name AS student_name,
                   sub.code AS sub_code, sub.name AS sub_name,
                   COALESCE(SUM(s.conducted_units), 0) AS conducted_units,
                   COALESCE(SUM(CASE WHEN ar.status IN ('PRESENT', 'LATE', 'DUTY_LEAVE', 'MEDICAL_LEAVE')
                                     THEN s.conducted_units ELSE 0 END), 0) AS attended_units
            FROM enrollments e
            JOIN students st ON st.id = e.student_id
            JOIN sessions s ON s.section_id = :sectionId AND s.subject_id = :subjectId
                            AND s.academic_period_id = :periodId AND s.status = 'CONDUCTED'
            JOIN subjects sub ON sub.id = s.subject_id
            LEFT JOIN attendance_records ar ON ar.session_id = s.id AND ar.student_id = st.id
            WHERE e.section_id = :sectionId AND e.status = 'ACTIVE'
            GROUP BY st.id, st.registration_number, st.name, sub.code, sub.name
            ORDER BY st.registration_number
        """;

        Query query = entityManager.createNativeQuery(sql)
            .setParameter("sectionId", sectionId)
            .setParameter("subjectId", subjectId)
            .setParameter("periodId", academicPeriodId);

        List<Object[]> results = query.getResultList();
        List<PredictionReportRow> rows = new ArrayList<>(results.size());
        BigDecimal effThreshold = targetThreshold != null ? targetThreshold : new BigDecimal("75.00");

        for (Object[] r : results) {
            UUID stId = (UUID) r[0];
            String regNo = (String) r[1];
            String stName = (String) r[2];
            String subCode = (String) r[3];
            String subName = (String) r[4];
            int conducted = ((Number) r[5]).intValue();
            int attended = ((Number) r[6]).intValue();

            BigDecimal currentPct = calculatePercentage(conducted, attended);

            // Prediction mathematics:
            int newTotal = conducted + projectedFutureSessions;
            BigDecimal requiredAttendedTotal = effThreshold.multiply(BigDecimal.valueOf(newTotal))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.CEILING);
            int requiredFuture = Math.max(0, requiredAttendedTotal.intValue() - attended);

            String feasibility;
            if (requiredFuture == 0) {
                feasibility = "ALREADY_MET";
            } else if (requiredFuture <= projectedFutureSessions) {
                feasibility = "ACHIEVABLE";
            } else {
                feasibility = "MATHEMATICALLY_IMPOSSIBLE";
            }

            rows.add(new PredictionReportRow(
                stId, regNo, stName, subCode, subName, conducted, attended, currentPct, effThreshold,
                projectedFutureSessions, requiredFuture, feasibility
            ));
        }
        return rows;
    }

    private BigDecimal calculatePercentage(int conducted, int attended) {
        if (conducted <= 0) {
            return new BigDecimal("100.00");
        }
        return BigDecimal.valueOf(attended)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(conducted), 2, RoundingMode.HALF_UP);
    }

    private String determineStatus(BigDecimal percentage, BigDecimal threshold, BigDecimal condonable) {
        if (percentage.compareTo(threshold) >= 0) {
            return "ELIGIBLE";
        }
        if (percentage.compareTo(condonable) >= 0) {
            return "CONDONABLE";
        }
        return "DEFAULTER";
    }
}
