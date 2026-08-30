package com.amcs.infrastructure.web.controller;

import com.amcs.application.dto.attendance.AttendanceCorrectionResponse;
import com.amcs.application.dto.attendance.AttendanceRecordItemDto;
import com.amcs.application.dto.attendance.CorrectAttendanceRecordRequest;
import com.amcs.application.dto.attendance.RecordAttendanceBatchRequest;
import com.amcs.application.dto.attendance.SessionAttendanceSummaryResponse;
import com.amcs.application.exception.ResourceNotFoundException;
import com.amcs.application.exception.SessionStateConflictException;
import com.amcs.application.exception.StudentNotEligibleException;
import com.amcs.application.service.AttendanceRecordingApplicationService;
import com.amcs.infrastructure.web.error.GlobalRestExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AttendanceControllerTest {

    @Mock private AttendanceRecordingApplicationService attendanceService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final UUID sessionId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID recordId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        AttendanceController controller = new AttendanceController(attendanceService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalRestExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{sessionId}/attendance returns 200 on successful roll-call")
    void shouldRecordAttendanceBatchSuccessfully() throws Exception {
        RecordAttendanceBatchRequest request = new RecordAttendanceBatchRequest(List.of(
            new AttendanceRecordItemDto(studentId, "PRESENT")
        ));

        SessionAttendanceSummaryResponse summary = new SessionAttendanceSummaryResponse(
            sessionId, UUID.randomUUID(), UUID.randomUUID(), LocalDate.now(), "THEORY", 1, "CONDUCTED", 1, 1, 0, 0, List.of());

        when(attendanceService.recordAttendance(eq(sessionId), any())).thenReturn(summary);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONDUCTED"))
            .andExpect(jsonPath("$.totalRecords").value(1))
            .andExpect(jsonPath("$.presentCount").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{sessionId}/attendance returns 409 when payload conflicts with conducted session")
    void shouldReturn409WhenConductedSessionConflict() throws Exception {
        RecordAttendanceBatchRequest request = new RecordAttendanceBatchRequest(List.of(
            new AttendanceRecordItemDto(studentId, "ABSENT")
        ));

        when(attendanceService.recordAttendance(eq(sessionId), any()))
            .thenThrow(new SessionStateConflictException("SESSION_ALREADY_CONDUCTED"));

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("SESSION_ALREADY_CONDUCTED"));
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{sessionId}/attendance returns 422 when student is not eligible")
    void shouldReturn422WhenStudentNotEligible() throws Exception {
        RecordAttendanceBatchRequest request = new RecordAttendanceBatchRequest(List.of(
            new AttendanceRecordItemDto(studentId, "PRESENT")
        ));

        when(attendanceService.recordAttendance(eq(sessionId), any()))
            .thenThrow(new StudentNotEligibleException("Student not enrolled"));

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("STUDENT_NOT_ELIGIBLE"));
    }

    @Test
    @DisplayName("POST /api/v1/attendance/records/{recordId}/correction returns 200 on valid correction")
    void shouldReturn200OnCorrection() throws Exception {
        CorrectAttendanceRecordRequest request = new CorrectAttendanceRecordRequest(
            "PRESENT", "Student presented duty certificate", UUID.randomUUID());

        AttendanceCorrectionResponse response = new AttendanceCorrectionResponse(
            recordId, sessionId, studentId, "ABSENT", "PRESENT", request.reason(), request.approverId(), Instant.now());

        when(attendanceService.correctAttendanceRecord(eq(recordId), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/attendance/records/{recordId}/correction", recordId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.previousStatus").value("ABSENT"))
            .andExpect(jsonPath("$.newStatus").value("PRESENT"));
    }

    @Test
    @DisplayName("GET /api/v1/sessions/{sessionId}/attendance returns 404 when session not found")
    void shouldReturn404WhenSessionNotFound() throws Exception {
        when(attendanceService.getSessionAttendance(sessionId))
            .thenThrow(new ResourceNotFoundException("Session not found"));

        mockMvc.perform(get("/api/v1/sessions/{sessionId}/attendance", sessionId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
