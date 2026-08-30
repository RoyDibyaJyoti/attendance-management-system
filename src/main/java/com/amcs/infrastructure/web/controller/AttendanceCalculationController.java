package com.amcs.infrastructure.web.controller;

import com.amcs.application.dto.calculation.OverallAttendanceSummaryResponse;
import com.amcs.application.dto.calculation.ShortageProjectionResponse;
import com.amcs.application.dto.calculation.StudentAttendanceOverviewResponse;
import com.amcs.application.dto.calculation.SubjectAttendanceSummaryResponse;
import com.amcs.application.service.AttendanceCalculationApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/students/{studentId}/attendance")
@Tag(name = "Attendance Calculations", description = "Subject attendance, overall aggregation, and future shortage projections")
public class AttendanceCalculationController {

    private final AttendanceCalculationApplicationService calculationService;

    public AttendanceCalculationController(AttendanceCalculationApplicationService calculationService) {
        this.calculationService = Objects.requireNonNull(calculationService, "calculationService");
    }

    @GetMapping("/subjects/{subjectId}")
    @Operation(summary = "Calculate student attendance percentage and status for a specific subject")
    public ResponseEntity<SubjectAttendanceSummaryResponse> calculateSubjectAttendance(
        @PathVariable UUID studentId,
        @PathVariable UUID subjectId,
        @RequestParam UUID sectionId,
        @RequestParam UUID policyId,
        @RequestParam UUID periodId
    ) {
        return ResponseEntity.ok(calculationService.calculateSubjectAttendance(
            studentId, subjectId, sectionId, policyId, periodId));
    }

    @GetMapping("/summary")
    @Operation(summary = "Calculate student attendance across all enrolled subjects for an academic period")
    public ResponseEntity<StudentAttendanceOverviewResponse> getStudentOverview(
        @PathVariable UUID studentId,
        @RequestParam UUID sectionId,
        @RequestParam UUID policyId,
        @RequestParam UUID periodId
    ) {
        return ResponseEntity.ok(calculationService.getStudentAttendanceOverview(
            studentId, sectionId, policyId, periodId));
    }

    @GetMapping("/overall")
    @Operation(summary = "Calculate overall institutional attendance percentage across all enrolled subjects")
    public ResponseEntity<OverallAttendanceSummaryResponse> calculateOverallAttendance(
        @PathVariable UUID studentId,
        @RequestParam UUID sectionId,
        @RequestParam UUID policyId,
        @RequestParam UUID periodId,
        @RequestParam(required = false) String strategy
    ) {
        return ResponseEntity.ok(calculationService.calculateOverallAttendance(
            studentId, sectionId, policyId, periodId, strategy));
    }

    @GetMapping("/subjects/{subjectId}/shortage")
    @Operation(summary = "Calculate shortage analysis and future predictive attendance projections")
    public ResponseEntity<ShortageProjectionResponse> calculateShortageProjection(
        @PathVariable UUID studentId,
        @PathVariable UUID subjectId,
        @RequestParam UUID sectionId,
        @RequestParam UUID policyId,
        @RequestParam UUID periodId,
        @RequestParam(defaultValue = "10") int projectedRemainingUnits
    ) {
        return ResponseEntity.ok(calculationService.calculateShortageProjection(
            studentId, subjectId, sectionId, policyId, periodId, projectedRemainingUnits));
    }
}
