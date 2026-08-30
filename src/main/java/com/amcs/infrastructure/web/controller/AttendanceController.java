package com.amcs.infrastructure.web.controller;

import com.amcs.application.dto.attendance.AttendanceCorrectionResponse;
import com.amcs.application.dto.attendance.CorrectAttendanceRecordRequest;
import com.amcs.application.dto.attendance.RecordAttendanceBatchRequest;
import com.amcs.application.dto.attendance.SessionAttendanceSummaryResponse;
import com.amcs.application.service.AttendanceRecordingApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@Tag(name = "Attendance Recording", description = "Atomic roll-call submission, session attendance sheets, and corrections")
public class AttendanceController {

    private final AttendanceRecordingApplicationService attendanceService;

    public AttendanceController(AttendanceRecordingApplicationService attendanceService) {
        this.attendanceService = Objects.requireNonNull(attendanceService, "attendanceService");
    }

    @PostMapping("/api/v1/sessions/{sessionId}/attendance")
    @Operation(summary = "Atomically record attendance for a session (idempotent for identical retries)")
    public ResponseEntity<SessionAttendanceSummaryResponse> recordAttendance(
        @PathVariable UUID sessionId,
        @Valid @RequestBody RecordAttendanceBatchRequest request
    ) {
        SessionAttendanceSummaryResponse response = attendanceService.recordAttendance(sessionId, request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/api/v1/sessions/{sessionId}/attendance")
    @Operation(summary = "Get roll-call attendance sheet for a session")
    public ResponseEntity<SessionAttendanceSummaryResponse> getSessionAttendance(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(attendanceService.getSessionAttendance(sessionId));
    }

    @PostMapping("/api/v1/attendance/records/{recordId}/correction")
    @Operation(summary = "Submit an audited attendance record correction (requires justification reason)")
    public ResponseEntity<AttendanceCorrectionResponse> correctAttendance(
        @PathVariable UUID recordId,
        @Valid @RequestBody CorrectAttendanceRecordRequest request
    ) {
        return ResponseEntity.ok(attendanceService.correctAttendanceRecord(recordId, request));
    }
}
