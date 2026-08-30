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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceReportApplicationService Unit Tests")
class AttendanceReportApplicationServiceTest {

    @Mock private AttendanceReportDataPort reportDataPort;
    @Mock private ReportWorkbookGeneratorPort reportWorkbookGeneratorPort;
    @Mock private ApplicationAuthorizationService authorizationService;

    private AttendanceReportApplicationService service;

    private final UUID studentId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();
    private final UUID sectionId = UUID.randomUUID();
    private final UUID academicPeriodId = UUID.randomUUID();
    private final UUID facultyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AttendanceReportApplicationService(
            reportDataPort,
            reportWorkbookGeneratorPort,
            authorizationService
        );
    }

    @Test
    @DisplayName("RPT-001: exports student attendance report")
    void shouldExportRpt001() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        List<StudentAttendanceReportRow> rows = List.of(
            new StudentAttendanceReportRow(subjectId, "CS101", "Programming", "THEORY", 20, 18, new BigDecimal("90.00"), "ELIGIBLE")
        );
        when(reportDataPort.getStudentAttendanceReport(studentId, academicPeriodId, sectionId)).thenReturn(rows);

        service.exportRpt001StudentAttendance(studentId, academicPeriodId, sectionId, os);

        verify(authorizationService).requireReport001Access(studentId);
        verify(reportWorkbookGeneratorPort).writeStudentAttendanceReport(eq(rows), eq(os));
    }

    @Test
    @DisplayName("RPT-002: exports subject attendance summary")
    void shouldExportRpt002() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        List<SubjectAttendanceSummaryReportRow> rows = List.of(
            new SubjectAttendanceSummaryReportRow(studentId, "CS01", "Alice", "CSE", 20, 16, new BigDecimal("80.00"), "ELIGIBLE")
        );
        when(reportDataPort.getSubjectAttendanceSummary(subjectId, sectionId, academicPeriodId)).thenReturn(rows);

        service.exportRpt002SubjectSummary(subjectId, sectionId, academicPeriodId, os);

        verify(authorizationService).requireSubjectSectionReportAccess(subjectId, sectionId, academicPeriodId, "Subject Attendance Summary");
        verify(reportWorkbookGeneratorPort).writeSubjectAttendanceSummaryReport(eq(rows), eq(os));
    }

    @Test
    @DisplayName("RPT-003: exports defaulters report")
    void shouldExportRpt003() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BigDecimal threshold = new BigDecimal("75.00");
        List<DefaulterReportRow> rows = List.of(
            new DefaulterReportRow(studentId, "CS01", "Alice", "CS101", "Programming", "A", new BigDecimal("60.00"), threshold, 3, 5)
        );
        when(reportDataPort.getDefaulterReport(academicPeriodId, sectionId, subjectId, threshold)).thenReturn(rows);

        service.exportRpt003Defaulters(academicPeriodId, sectionId, subjectId, threshold, os);

        verify(authorizationService).requireSubjectSectionReportAccess(subjectId, sectionId, academicPeriodId, "Defaulter Report");
        verify(reportWorkbookGeneratorPort).writeDefaulterReport(eq(rows), eq(os));
    }

    @Test
    @DisplayName("RPT-004: exports attendance register")
    void shouldExportRpt004() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        LocalDate now = LocalDate.now();
        AttendanceRegisterReportData data = new AttendanceRegisterReportData(
            "CS101", "Programming", "A", List.of(now), List.of()
        );
        when(reportDataPort.getAttendanceRegisterReport(subjectId, sectionId, academicPeriodId, now, now)).thenReturn(data);

        service.exportRpt004AttendanceRegister(subjectId, sectionId, academicPeriodId, now, now, os);

        verify(authorizationService).requireSubjectSectionReportAccess(subjectId, sectionId, academicPeriodId, "Attendance Register");
        verify(reportWorkbookGeneratorPort).writeAttendanceRegisterReport(eq(data), eq(os));
    }

    @Test
    @DisplayName("RPT-005: exports overall attendance summary")
    void shouldExportRpt005() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BigDecimal threshold = new BigDecimal("75.00");
        List<OverallAttendanceReportRow> rows = List.of(
            new OverallAttendanceReportRow(studentId, "CS01", "Alice", "A", 5, 100, 85, new BigDecimal("85.00"), threshold, "ELIGIBLE")
        );
        when(reportDataPort.getOverallAttendanceSummary(academicPeriodId, sectionId, null, threshold)).thenReturn(rows);

        service.exportRpt005OverallSummary(academicPeriodId, sectionId, null, threshold, os);

        verify(authorizationService).requireSectionReportAccess(sectionId, academicPeriodId, "Overall Attendance Summary");
        verify(reportWorkbookGeneratorPort).writeOverallAttendanceSummaryReport(eq(rows), eq(os));
    }

    @Test
    @DisplayName("RPT-006: exports faculty marking compliance report")
    void shouldExportRpt006() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        List<FacultyComplianceReportRow> rows = List.of(
            new FacultyComplianceReportRow(facultyId, "EMP01", "Dr. Smith", "CSE", "CS101", "Prog", "A", 30, 28, 2, new BigDecimal("93.33"))
        );
        when(reportDataPort.getFacultyComplianceReport(academicPeriodId, facultyId, null)).thenReturn(rows);

        service.exportRpt006FacultyCompliance(academicPeriodId, facultyId, null, os);

        verify(authorizationService).requireFacultyComplianceReportAccess(facultyId);
        verify(reportWorkbookGeneratorPort).writeFacultyComplianceReport(eq(rows), eq(os));
    }

    @Test
    @DisplayName("RPT-007: exports condonation register report")
    void shouldExportRpt007() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        List<CondonationRegisterReportRow> rows = List.of();
        when(reportDataPort.getCondonationRegisterReport(academicPeriodId, sectionId, subjectId, "DUTY_LEAVE", null, null)).thenReturn(rows);

        service.exportRpt007CondonationRegister(academicPeriodId, sectionId, subjectId, "DUTY_LEAVE", null, null, os);

        verify(authorizationService).requireSubjectSectionReportAccess(subjectId, sectionId, academicPeriodId, "Condonation Register");
        verify(reportWorkbookGeneratorPort).writeCondonationRegisterReport(eq(rows), eq(os));
    }

    @Test
    @DisplayName("RPT-008: exports prediction report")
    void shouldExportRpt008() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BigDecimal threshold = new BigDecimal("75.00");
        List<PredictionReportRow> rows = List.of(
            new PredictionReportRow(studentId, "CS01", "Alice", "CS101", "Prog", 20, 12, new BigDecimal("60.00"), threshold, 20, 6, "ACHIEVABLE")
        );
        when(reportDataPort.getPredictionReport(subjectId, sectionId, academicPeriodId, 20, threshold)).thenReturn(rows);

        service.exportRpt008Prediction(subjectId, sectionId, academicPeriodId, 20, threshold, os);

        verify(authorizationService).requireSubjectSectionReportAccess(subjectId, sectionId, academicPeriodId, "Prediction Report");
        verify(reportWorkbookGeneratorPort).writePredictionReport(eq(rows), eq(os));
    }
}
