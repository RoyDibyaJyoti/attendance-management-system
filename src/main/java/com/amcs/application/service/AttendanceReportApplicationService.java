package com.amcs.application.service;

import com.amcs.application.dto.report.AttendanceRegisterReportData;
import com.amcs.application.dto.report.CondonationRegisterReportRow;
import com.amcs.application.dto.report.DefaulterReportRow;
import com.amcs.application.dto.report.FacultyComplianceReportRow;
import com.amcs.application.dto.report.OverallAttendanceReportRow;
import com.amcs.application.dto.report.PredictionReportRow;
import com.amcs.application.dto.report.StudentAttendanceReportRow;
import com.amcs.application.dto.report.SubjectAttendanceSummaryReportRow;
import com.amcs.application.port.out.report.AttendanceReportDataPort;
import com.amcs.application.port.out.report.ReportWorkbookGeneratorPort;
import com.amcs.application.security.ApplicationAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AttendanceReportApplicationService {

    private final AttendanceReportDataPort reportDataPort;
    private final ReportWorkbookGeneratorPort reportWorkbookGeneratorPort;
    private final ApplicationAuthorizationService authorizationService;

    public AttendanceReportApplicationService(
        AttendanceReportDataPort reportDataPort,
        ReportWorkbookGeneratorPort reportWorkbookGeneratorPort,
        ApplicationAuthorizationService authorizationService
    ) {
        this.reportDataPort = Objects.requireNonNull(reportDataPort, "reportDataPort");
        this.reportWorkbookGeneratorPort = Objects.requireNonNull(reportWorkbookGeneratorPort, "reportWorkbookGeneratorPort");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService");
    }

    public void verifyRpt001Access(UUID studentId) {
        authorizationService.requireReport001Access(studentId);
    }

    public void verifySubjectSectionAccess(UUID subjectId, UUID sectionId, UUID academicPeriodId, String reportName) {
        authorizationService.requireSubjectSectionReportAccess(subjectId, sectionId, academicPeriodId, reportName);
    }

    public void verifyDefaulterAccess(UUID academicPeriodId, UUID sectionId, UUID subjectId) {
        if (subjectId != null && sectionId != null) {
            authorizationService.requireSubjectSectionReportAccess(subjectId, sectionId, academicPeriodId, "Defaulter Report");
        } else if (sectionId != null) {
            authorizationService.requireSectionReportAccess(sectionId, academicPeriodId, "Defaulter Report");
        } else {
            authorizationService.requireAdminOnly("access institutional defaulter report");
        }
    }

    public void verifyOverallAccess(UUID academicPeriodId, UUID sectionId) {
        if (sectionId != null) {
            authorizationService.requireSectionReportAccess(sectionId, academicPeriodId, "Overall Attendance Summary");
        } else {
            authorizationService.requireAdminOnly("access overall attendance summary for department/institution");
        }
    }

    public void verifyFacultyComplianceAccess(UUID facultyId) {
        authorizationService.requireFacultyComplianceReportAccess(facultyId);
    }

    public void verifyCondonationAccess(UUID academicPeriodId, UUID sectionId, UUID subjectId) {
        if (subjectId != null && sectionId != null) {
            authorizationService.requireSubjectSectionReportAccess(subjectId, sectionId, academicPeriodId, "Condonation Register");
        } else if (sectionId != null) {
            authorizationService.requireSectionReportAccess(sectionId, academicPeriodId, "Condonation Register");
        } else {
            authorizationService.requireAdminOnly("access institutional condonation register");
        }
    }

    /**
     * RPT-001: Student Attendance Report (per-student, per-subject, per-semester).
     */
    public void exportRpt001StudentAttendance(
        UUID studentId, UUID academicPeriodId, UUID sectionId, OutputStream outputStream
    ) {
        Objects.requireNonNull(studentId, "studentId must not be null");
        Objects.requireNonNull(academicPeriodId, "academicPeriodId must not be null");
        Objects.requireNonNull(outputStream, "outputStream must not be null");

        authorizationService.requireReport001Access(studentId);
        List<StudentAttendanceReportRow> rows = reportDataPort.getStudentAttendanceReport(studentId, academicPeriodId, sectionId);
        reportWorkbookGeneratorPort.writeStudentAttendanceReport(rows, outputStream);
    }

    /**
     * RPT-002: Subject Attendance Summary (per-subject, per-section roster).
     */
    public void exportRpt002SubjectSummary(
        UUID subjectId, UUID sectionId, UUID academicPeriodId, OutputStream outputStream
    ) {
        Objects.requireNonNull(subjectId, "subjectId must not be null");
        Objects.requireNonNull(sectionId, "sectionId must not be null");
        Objects.requireNonNull(academicPeriodId, "academicPeriodId must not be null");
        Objects.requireNonNull(outputStream, "outputStream must not be null");

        authorizationService.requireSubjectSectionReportAccess(subjectId, sectionId, academicPeriodId, "Subject Attendance Summary");
        List<SubjectAttendanceSummaryReportRow> rows = reportDataPort.getSubjectAttendanceSummary(subjectId, sectionId, academicPeriodId);
        reportWorkbookGeneratorPort.writeSubjectAttendanceSummaryReport(rows, outputStream);
    }

    /**
     * RPT-003: Defaulter Report (students below threshold).
     */
    public void exportRpt003Defaulters(
        UUID academicPeriodId, UUID sectionId, UUID subjectId, BigDecimal threshold, OutputStream outputStream
    ) {
        Objects.requireNonNull(academicPeriodId, "academicPeriodId must not be null");
        Objects.requireNonNull(outputStream, "outputStream must not be null");

        BigDecimal effectiveThreshold = threshold != null ? threshold : new BigDecimal("75.00");
        if (subjectId != null && sectionId != null) {
            authorizationService.requireSubjectSectionReportAccess(subjectId, sectionId, academicPeriodId, "Defaulter Report");
        } else if (sectionId != null) {
            authorizationService.requireSectionReportAccess(sectionId, academicPeriodId, "Defaulter Report");
        } else {
            authorizationService.requireAdminOnly("access institutional defaulter report");
        }

        List<DefaulterReportRow> rows = reportDataPort.getDefaulterReport(academicPeriodId, sectionId, subjectId, effectiveThreshold);
        reportWorkbookGeneratorPort.writeDefaulterReport(rows, outputStream);
    }

    /**
     * RPT-004: Attendance Register (date-wise attendance grid).
     */
    public void exportRpt004AttendanceRegister(
        UUID subjectId, UUID sectionId, UUID academicPeriodId, LocalDate startDate, LocalDate endDate, OutputStream outputStream
    ) {
        Objects.requireNonNull(subjectId, "subjectId must not be null");
        Objects.requireNonNull(sectionId, "sectionId must not be null");
        Objects.requireNonNull(academicPeriodId, "academicPeriodId must not be null");
        Objects.requireNonNull(outputStream, "outputStream must not be null");

        authorizationService.requireSubjectSectionReportAccess(subjectId, sectionId, academicPeriodId, "Attendance Register");
        AttendanceRegisterReportData data = reportDataPort.getAttendanceRegisterReport(subjectId, sectionId, academicPeriodId, startDate, endDate);
        reportWorkbookGeneratorPort.writeAttendanceRegisterReport(data, outputStream);
    }

    /**
     * RPT-005: Overall Attendance Summary (per-student overall attendance across subjects).
     */
    public void exportRpt005OverallSummary(
        UUID academicPeriodId, UUID sectionId, UUID departmentId, BigDecimal threshold, OutputStream outputStream
    ) {
        Objects.requireNonNull(academicPeriodId, "academicPeriodId must not be null");
        Objects.requireNonNull(outputStream, "outputStream must not be null");

        BigDecimal effectiveThreshold = threshold != null ? threshold : new BigDecimal("75.00");
        if (sectionId != null) {
            authorizationService.requireSectionReportAccess(sectionId, academicPeriodId, "Overall Attendance Summary");
        } else {
            authorizationService.requireAdminOnly("access overall attendance summary for department/institution");
        }

        List<OverallAttendanceReportRow> rows = reportDataPort.getOverallAttendanceSummary(academicPeriodId, sectionId, departmentId, effectiveThreshold);
        reportWorkbookGeneratorPort.writeOverallAttendanceSummaryReport(rows, outputStream);
    }

    /**
     * RPT-006: Faculty Marking Compliance Report (sessions marked vs. unmarked).
     */
    public void exportRpt006FacultyCompliance(
        UUID academicPeriodId, UUID facultyId, UUID departmentId, OutputStream outputStream
    ) {
        Objects.requireNonNull(academicPeriodId, "academicPeriodId must not be null");
        Objects.requireNonNull(outputStream, "outputStream must not be null");

        authorizationService.requireFacultyComplianceReportAccess(facultyId);
        List<FacultyComplianceReportRow> rows = reportDataPort.getFacultyComplianceReport(academicPeriodId, facultyId, departmentId);
        reportWorkbookGeneratorPort.writeFacultyComplianceReport(rows, outputStream);
    }

    /**
     * RPT-007: Condonation / Duty Leave Register (records with DUTY_LEAVE or MEDICAL_LEAVE).
     */
    public void exportRpt007CondonationRegister(
        UUID academicPeriodId, UUID sectionId, UUID subjectId, String leaveType, LocalDate startDate, LocalDate endDate, OutputStream outputStream
    ) {
        Objects.requireNonNull(academicPeriodId, "academicPeriodId must not be null");
        Objects.requireNonNull(outputStream, "outputStream must not be null");

        if (subjectId != null && sectionId != null) {
            authorizationService.requireSubjectSectionReportAccess(subjectId, sectionId, academicPeriodId, "Condonation Register");
        } else if (sectionId != null) {
            authorizationService.requireSectionReportAccess(sectionId, academicPeriodId, "Condonation Register");
        } else {
            authorizationService.requireAdminOnly("access institutional condonation register");
        }

        List<CondonationRegisterReportRow> rows = reportDataPort.getCondonationRegisterReport(academicPeriodId, sectionId, subjectId, leaveType, startDate, endDate);
        reportWorkbookGeneratorPort.writeCondonationRegisterReport(rows, outputStream);
    }

    /**
     * RPT-008: Prediction Report (future attendance required to reach threshold).
     */
    public void exportRpt008Prediction(
        UUID subjectId, UUID sectionId, UUID academicPeriodId, Integer futureSessions, BigDecimal threshold, OutputStream outputStream
    ) {
        Objects.requireNonNull(subjectId, "subjectId must not be null");
        Objects.requireNonNull(sectionId, "sectionId must not be null");
        Objects.requireNonNull(academicPeriodId, "academicPeriodId must not be null");
        Objects.requireNonNull(outputStream, "outputStream must not be null");

        int projectedFuture = (futureSessions != null && futureSessions > 0) ? futureSessions : 20;
        BigDecimal targetThreshold = threshold != null ? threshold : new BigDecimal("75.00");

        authorizationService.requireSubjectSectionReportAccess(subjectId, sectionId, academicPeriodId, "Prediction Report");
        List<PredictionReportRow> rows = reportDataPort.getPredictionReport(subjectId, sectionId, academicPeriodId, projectedFuture, targetThreshold);
        reportWorkbookGeneratorPort.writePredictionReport(rows, outputStream);
    }
}
