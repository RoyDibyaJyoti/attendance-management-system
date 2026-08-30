package com.amcs.infrastructure.web.controller;

import com.amcs.application.service.AttendanceReportApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Attendance Report Export Pipeline", description = "Endpoints for streaming XLSX exports of RPT-001 through RPT-008")
public class AttendanceReportController {

    private static final String XLSX_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final AttendanceReportApplicationService reportService;

    public AttendanceReportController(AttendanceReportApplicationService reportService) {
        this.reportService = Objects.requireNonNull(reportService, "reportService");
    }

    @GetMapping({"/rpt-001/student-attendance", "/rpt-001"})
    @Operation(summary = "RPT-001: Student Attendance Report", description = "Exports per-student, per-subject attendance report for a semester")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "XLSX report binary", content = @Content(mediaType = XLSX_MEDIA_TYPE)),
        @ApiResponse(responseCode = "401", description = "Unauthenticated"),
        @ApiResponse(responseCode = "403", description = "Access denied (Student may only view own report)"),
        @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    public void exportRpt001(
        @Parameter(description = "Target student UUID") @RequestParam("studentId") UUID studentId,
        @Parameter(description = "Academic period UUID") @RequestParam("academicPeriodId") UUID academicPeriodId,
        @Parameter(description = "Optional section UUID") @RequestParam(value = "sectionId", required = false) UUID sectionId,
        HttpServletResponse response
    ) throws IOException {
        reportService.verifyRpt001Access(studentId);
        setDownloadHeaders(response, "rpt-001-student-attendance-" + studentId + ".xlsx");
        reportService.exportRpt001StudentAttendance(studentId, academicPeriodId, sectionId, response.getOutputStream());
    }

    @GetMapping({"/rpt-002/subject-summary", "/rpt-002"})
    @Operation(summary = "RPT-002: Subject Attendance Summary", description = "Exports per-subject, per-section attendance roster")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "XLSX report binary", content = @Content(mediaType = XLSX_MEDIA_TYPE)),
        @ApiResponse(responseCode = "401", description = "Unauthenticated"),
        @ApiResponse(responseCode = "403", description = "Access denied (Faculty assigned only, HOD_ADMIN allowed)"),
        @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    public void exportRpt002(
        @Parameter(description = "Subject UUID") @RequestParam("subjectId") UUID subjectId,
        @Parameter(description = "Section UUID") @RequestParam("sectionId") UUID sectionId,
        @Parameter(description = "Academic period UUID") @RequestParam("academicPeriodId") UUID academicPeriodId,
        HttpServletResponse response
    ) throws IOException {
        reportService.verifySubjectSectionAccess(subjectId, sectionId, academicPeriodId, "Subject Attendance Summary");
        setDownloadHeaders(response, "rpt-002-subject-summary.xlsx");
        reportService.exportRpt002SubjectSummary(subjectId, sectionId, academicPeriodId, response.getOutputStream());
    }

    @GetMapping({"/rpt-003/defaulters", "/rpt-003"})
    @Operation(summary = "RPT-003: Defaulter Report", description = "Exports students below threshold with shortage metrics and required classes")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "XLSX report binary", content = @Content(mediaType = XLSX_MEDIA_TYPE)),
        @ApiResponse(responseCode = "401", description = "Unauthenticated"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    public void exportRpt003(
        @Parameter(description = "Academic period UUID") @RequestParam("academicPeriodId") UUID academicPeriodId,
        @Parameter(description = "Optional section UUID") @RequestParam(value = "sectionId", required = false) UUID sectionId,
        @Parameter(description = "Optional subject UUID") @RequestParam(value = "subjectId", required = false) UUID subjectId,
        @Parameter(description = "Threshold percentage (default: 75.00)") @RequestParam(value = "threshold", defaultValue = "75.00") BigDecimal threshold,
        HttpServletResponse response
    ) throws IOException {
        reportService.verifyDefaulterAccess(academicPeriodId, sectionId, subjectId);
        setDownloadHeaders(response, "rpt-003-defaulters.xlsx");
        reportService.exportRpt003Defaulters(academicPeriodId, sectionId, subjectId, threshold, response.getOutputStream());
    }

    @GetMapping({"/rpt-004/attendance-register", "/rpt-004"})
    @Operation(summary = "RPT-004: Attendance Register", description = "Exports date-wise attendance grid for students across conducted sessions")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "XLSX report binary", content = @Content(mediaType = XLSX_MEDIA_TYPE)),
        @ApiResponse(responseCode = "401", description = "Unauthenticated"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    public void exportRpt004(
        @Parameter(description = "Subject UUID") @RequestParam("subjectId") UUID subjectId,
        @Parameter(description = "Section UUID") @RequestParam("sectionId") UUID sectionId,
        @Parameter(description = "Academic period UUID") @RequestParam("academicPeriodId") UUID academicPeriodId,
        @Parameter(description = "Optional start date") @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @Parameter(description = "Optional end date") @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        HttpServletResponse response
    ) throws IOException {
        reportService.verifySubjectSectionAccess(subjectId, sectionId, academicPeriodId, "Attendance Register");
        setDownloadHeaders(response, "rpt-004-attendance-register.xlsx");
        reportService.exportRpt004AttendanceRegister(subjectId, sectionId, academicPeriodId, startDate, endDate, response.getOutputStream());
    }

    @GetMapping({"/rpt-005/overall-summary", "/rpt-005"})
    @Operation(summary = "RPT-005: Overall Attendance Summary", description = "Exports per-student aggregate attendance across all enrolled subjects")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "XLSX report binary", content = @Content(mediaType = XLSX_MEDIA_TYPE)),
        @ApiResponse(responseCode = "401", description = "Unauthenticated"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    public void exportRpt005(
        @Parameter(description = "Academic period UUID") @RequestParam("academicPeriodId") UUID academicPeriodId,
        @Parameter(description = "Optional section UUID") @RequestParam(value = "sectionId", required = false) UUID sectionId,
        @Parameter(description = "Optional department UUID") @RequestParam(value = "departmentId", required = false) UUID departmentId,
        @Parameter(description = "Threshold percentage (default: 75.00)") @RequestParam(value = "threshold", defaultValue = "75.00") BigDecimal threshold,
        HttpServletResponse response
    ) throws IOException {
        reportService.verifyOverallAccess(academicPeriodId, sectionId);
        setDownloadHeaders(response, "rpt-005-overall-summary.xlsx");
        reportService.exportRpt005OverallSummary(academicPeriodId, sectionId, departmentId, threshold, response.getOutputStream());
    }

    @GetMapping({"/rpt-006/faculty-compliance", "/rpt-006"})
    @Operation(summary = "RPT-006: Faculty Marking Compliance Report", description = "Exports sessions marked vs unmarked by faculty and subject")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "XLSX report binary", content = @Content(mediaType = XLSX_MEDIA_TYPE)),
        @ApiResponse(responseCode = "401", description = "Unauthenticated"),
        @ApiResponse(responseCode = "403", description = "Access denied (Faculty own data only, HOD_ADMIN unrestricted)"),
        @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    public void exportRpt006(
        @Parameter(description = "Academic period UUID") @RequestParam("academicPeriodId") UUID academicPeriodId,
        @Parameter(description = "Optional faculty UUID") @RequestParam(value = "facultyId", required = false) UUID facultyId,
        @Parameter(description = "Optional department UUID") @RequestParam(value = "departmentId", required = false) UUID departmentId,
        HttpServletResponse response
    ) throws IOException {
        reportService.verifyFacultyComplianceAccess(facultyId);
        setDownloadHeaders(response, "rpt-006-faculty-compliance.xlsx");
        reportService.exportRpt006FacultyCompliance(academicPeriodId, facultyId, departmentId, response.getOutputStream());
    }

    @GetMapping({"/rpt-007/condonation-register", "/rpt-007"})
    @Operation(summary = "RPT-007: Condonation / Duty Leave Register", description = "Exports records with DUTY_LEAVE or MEDICAL_LEAVE for administrative review")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "XLSX report binary", content = @Content(mediaType = XLSX_MEDIA_TYPE)),
        @ApiResponse(responseCode = "401", description = "Unauthenticated"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    public void exportRpt007(
        @Parameter(description = "Academic period UUID") @RequestParam("academicPeriodId") UUID academicPeriodId,
        @Parameter(description = "Optional section UUID") @RequestParam(value = "sectionId", required = false) UUID sectionId,
        @Parameter(description = "Optional subject UUID") @RequestParam(value = "subjectId", required = false) UUID subjectId,
        @Parameter(description = "Optional leave type filter (DUTY_LEAVE or MEDICAL_LEAVE)") @RequestParam(value = "leaveType", required = false) String leaveType,
        @Parameter(description = "Optional start date") @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @Parameter(description = "Optional end date") @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        HttpServletResponse response
    ) throws IOException {
        reportService.verifyCondonationAccess(academicPeriodId, sectionId, subjectId);
        setDownloadHeaders(response, "rpt-007-condonation-register.xlsx");
        reportService.exportRpt007CondonationRegister(academicPeriodId, sectionId, subjectId, leaveType, startDate, endDate, response.getOutputStream());
    }

    @GetMapping({"/rpt-008/prediction", "/rpt-008"})
    @Operation(summary = "RPT-008: Prediction Report", description = "Exports future attendance required to clear defaulter status for students")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "XLSX report binary", content = @Content(mediaType = XLSX_MEDIA_TYPE)),
        @ApiResponse(responseCode = "401", description = "Unauthenticated"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    public void exportRpt008(
        @Parameter(description = "Subject UUID") @RequestParam("subjectId") UUID subjectId,
        @Parameter(description = "Section UUID") @RequestParam("sectionId") UUID sectionId,
        @Parameter(description = "Academic period UUID") @RequestParam("academicPeriodId") UUID academicPeriodId,
        @Parameter(description = "Projected future sessions count (default: 20)") @RequestParam(value = "futureSessions", defaultValue = "20") Integer futureSessions,
        @Parameter(description = "Target threshold percentage (default: 75.00)") @RequestParam(value = "threshold", defaultValue = "75.00") BigDecimal threshold,
        HttpServletResponse response
    ) throws IOException {
        reportService.verifySubjectSectionAccess(subjectId, sectionId, academicPeriodId, "Prediction Report");
        setDownloadHeaders(response, "rpt-008-prediction.xlsx");
        reportService.exportRpt008Prediction(subjectId, sectionId, academicPeriodId, futureSessions, threshold, response.getOutputStream());
    }

    private void setDownloadHeaders(HttpServletResponse response, String filename) {
        response.setContentType(XLSX_MEDIA_TYPE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
    }
}
