package com.amcs.infrastructure.web.controller;

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
import com.amcs.application.service.AttendanceReportApplicationService;
import com.amcs.infrastructure.excel.adapter.ApachePoiReportWorkbookGeneratorAdapter;
import com.amcs.infrastructure.excel.generator.StreamingReportExcelGenerator;
import com.amcs.infrastructure.web.error.GlobalRestExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceReportController API Integration Tests")
class AttendanceReportControllerApiIntegrationTest {

    @Mock private AttendanceReportDataPort reportDataPort;
    @Mock private ApplicationAuthorizationService authorizationService;

    private MockMvc mockMvc;

    private final UUID studentId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();
    private final UUID sectionId = UUID.randomUUID();
    private final UUID academicPeriodId = UUID.randomUUID();
    private final UUID facultyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ReportWorkbookGeneratorPort reportGeneratorPort =
            new ApachePoiReportWorkbookGeneratorAdapter(new StreamingReportExcelGenerator());

        AttendanceReportApplicationService applicationService = new AttendanceReportApplicationService(
            reportDataPort,
            reportGeneratorPort,
            authorizationService
        );

        AttendanceReportController controller = new AttendanceReportController(applicationService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalRestExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("GET /rpt-001/student-attendance returns XLSX binary stream")
    void testRpt001Stream() throws Exception {
        when(reportDataPort.getStudentAttendanceReport(studentId, academicPeriodId, sectionId))
            .thenReturn(List.of(new StudentAttendanceReportRow(
                subjectId, "CS101", "Prog", "THEORY", 20, 18, new BigDecimal("90.00"), "ELIGIBLE"
            )));

        mockMvc.perform(get("/api/v1/reports/rpt-001/student-attendance")
                .param("studentId", studentId.toString())
                .param("academicPeriodId", academicPeriodId.toString())
                .param("sectionId", sectionId.toString()))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"rpt-001-student-attendance-" + studentId + ".xlsx\""));
    }

    @Test
    @DisplayName("GET /rpt-002/subject-summary returns XLSX binary stream")
    void testRpt002Stream() throws Exception {
        when(reportDataPort.getSubjectAttendanceSummary(subjectId, sectionId, academicPeriodId))
            .thenReturn(List.of(new SubjectAttendanceSummaryReportRow(
                studentId, "REG001", "Alice", "CSE", 20, 18, new BigDecimal("90.00"), "ELIGIBLE"
            )));

        mockMvc.perform(get("/api/v1/reports/rpt-002/subject-summary")
                .param("subjectId", subjectId.toString())
                .param("sectionId", sectionId.toString())
                .param("academicPeriodId", academicPeriodId.toString()))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"rpt-002-subject-summary.xlsx\""));
    }

    @Test
    @DisplayName("GET /rpt-003/defaulters returns XLSX binary stream")
    void testRpt003Stream() throws Exception {
        when(reportDataPort.getDefaulterReport(eq(academicPeriodId), eq(sectionId), eq(subjectId), any()))
            .thenReturn(List.of(new DefaulterReportRow(
                studentId, "REG001", "Bob", "CS101", "Prog", "A", new BigDecimal("50.00"), new BigDecimal("75.00"), 4, 6
            )));

        mockMvc.perform(get("/api/v1/reports/rpt-003/defaulters")
                .param("academicPeriodId", academicPeriodId.toString())
                .param("sectionId", sectionId.toString())
                .param("subjectId", subjectId.toString()))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"rpt-003-defaulters.xlsx\""));
    }

    @Test
    @DisplayName("GET /rpt-004/attendance-register returns XLSX binary stream")
    void testRpt004Stream() throws Exception {
        when(reportDataPort.getAttendanceRegisterReport(eq(subjectId), eq(sectionId), eq(academicPeriodId), any(), any()))
            .thenReturn(new AttendanceRegisterReportData("CS101", "Prog", "A", List.of(LocalDate.now()), List.of()));

        mockMvc.perform(get("/api/v1/reports/rpt-004/attendance-register")
                .param("subjectId", subjectId.toString())
                .param("sectionId", sectionId.toString())
                .param("academicPeriodId", academicPeriodId.toString()))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"rpt-004-attendance-register.xlsx\""));
    }

    @Test
    @DisplayName("GET /rpt-005/overall-summary returns XLSX binary stream")
    void testRpt005Stream() throws Exception {
        when(reportDataPort.getOverallAttendanceSummary(eq(academicPeriodId), eq(sectionId), any(), any()))
            .thenReturn(List.of(new OverallAttendanceReportRow(
                studentId, "REG001", "Alice", "A", 5, 100, 90, new BigDecimal("90.00"), new BigDecimal("75.00"), "ELIGIBLE"
            )));

        mockMvc.perform(get("/api/v1/reports/rpt-005/overall-summary")
                .param("academicPeriodId", academicPeriodId.toString())
                .param("sectionId", sectionId.toString()))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"rpt-005-overall-summary.xlsx\""));
    }

    @Test
    @DisplayName("GET /rpt-006/faculty-compliance returns XLSX binary stream")
    void testRpt006Stream() throws Exception {
        when(reportDataPort.getFacultyComplianceReport(eq(academicPeriodId), eq(facultyId), any()))
            .thenReturn(List.of(new FacultyComplianceReportRow(
                facultyId, "EMP01", "Prof Turing", "CSE", "CS101", "Prog", "A", 20, 20, 0, new BigDecimal("100.00")
            )));

        mockMvc.perform(get("/api/v1/reports/rpt-006/faculty-compliance")
                .param("academicPeriodId", academicPeriodId.toString())
                .param("facultyId", facultyId.toString()))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"rpt-006-faculty-compliance.xlsx\""));
    }

    @Test
    @DisplayName("GET /rpt-007/condonation-register returns XLSX binary stream")
    void testRpt007Stream() throws Exception {
        when(reportDataPort.getCondonationRegisterReport(eq(academicPeriodId), any(), any(), any(), any(), any()))
            .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/reports/rpt-007/condonation-register")
                .param("academicPeriodId", academicPeriodId.toString()))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"rpt-007-condonation-register.xlsx\""));
    }

    @Test
    @DisplayName("GET /rpt-008/prediction returns XLSX binary stream")
    void testRpt008Stream() throws Exception {
        when(reportDataPort.getPredictionReport(eq(subjectId), eq(sectionId), eq(academicPeriodId), any(Integer.class), any(BigDecimal.class)))
            .thenReturn(List.of(new PredictionReportRow(
                studentId, "REG001", "Alice", "CS101", "Prog", 20, 16, new BigDecimal("80.00"), new BigDecimal("75.00"), 20, 14, "ACHIEVABLE"
            )));

        mockMvc.perform(get("/api/v1/reports/rpt-008/prediction")
                .param("subjectId", subjectId.toString())
                .param("sectionId", sectionId.toString())
                .param("academicPeriodId", academicPeriodId.toString()))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"rpt-008-prediction.xlsx\""));
    }

    @Test
    @DisplayName("Missing required parameter returns 400 Bad Request")
    void testMissingParameterReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/reports/rpt-001/student-attendance"))
            .andExpect(status().isBadRequest());
    }
}
