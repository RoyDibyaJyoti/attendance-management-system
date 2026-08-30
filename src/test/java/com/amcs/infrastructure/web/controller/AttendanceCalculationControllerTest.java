package com.amcs.infrastructure.web.controller;

import com.amcs.application.dto.calculation.OverallAttendanceSummaryResponse;
import com.amcs.application.dto.calculation.ShortageProjectionResponse;
import com.amcs.application.dto.calculation.SubjectAttendanceSummaryResponse;
import com.amcs.application.service.AttendanceCalculationApplicationService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AttendanceCalculationControllerTest {

    @Mock private AttendanceCalculationApplicationService calculationService;

    private MockMvc mockMvc;

    private final UUID studentId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();
    private final UUID sectionId = UUID.randomUUID();
    private final UUID policyId = UUID.randomUUID();
    private final UUID periodId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        AttendanceCalculationController controller = new AttendanceCalculationController(calculationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalRestExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("GET /api/v1/students/{studentId}/attendance/subjects/{subjectId} returns 200 with calculations")
    void shouldCalculateSubjectAttendance() throws Exception {
        SubjectAttendanceSummaryResponse response = new SubjectAttendanceSummaryResponse(
            studentId, subjectId, "CS201", "Algorithms", "Standard Policy", 1,
            new BigDecimal("75.00"), new BigDecimal("20.00"), new BigDecimal("16.00"),
            new BigDecimal("80.00"), "ADEQUATE", true, false, BigDecimal.ZERO, new BigDecimal("1.00"), 0, false);

        when(calculationService.calculateSubjectAttendance(studentId, subjectId, sectionId, policyId, periodId))
            .thenReturn(response);

        mockMvc.perform(get("/api/v1/students/{studentId}/attendance/subjects/{subjectId}", studentId, subjectId)
                .param("sectionId", sectionId.toString())
                .param("policyId", policyId.toString())
                .param("periodId", periodId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subjectCode").value("CS201"))
            .andExpect(jsonPath("$.attendancePercentage").value(80.00))
            .andExpect(jsonPath("$.classification").value("ADEQUATE"))
            .andExpect(jsonPath("$.isAdequate").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/students/{studentId}/attendance/overall returns 200 with aggregated result")
    void shouldCalculateOverallAttendance() throws Exception {
        OverallAttendanceSummaryResponse response = new OverallAttendanceSummaryResponse(
            studentId, "Overall Policy", "ARITHMETIC_MEAN", new BigDecimal("75.00"),
            new BigDecimal("82.50"), "ADEQUATE", true, false, 5);

        when(calculationService.calculateOverallAttendance(studentId, sectionId, policyId, periodId, null))
            .thenReturn(response);

        mockMvc.perform(get("/api/v1/students/{studentId}/attendance/overall", studentId)
                .param("sectionId", sectionId.toString())
                .param("policyId", policyId.toString())
                .param("periodId", periodId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.overallPercentage").value(82.50))
            .andExpect(jsonPath("$.aggregationStrategy").value("ARITHMETIC_MEAN"));
    }

    @Test
    @DisplayName("GET /api/v1/students/{studentId}/attendance/subjects/{subjectId}/shortage returns 200 with projections")
    void shouldCalculateShortageProjection() throws Exception {
        ShortageProjectionResponse response = new ShortageProjectionResponse(
            studentId, subjectId, new BigDecimal("60.00"), new BigDecimal("75.00"),
            new BigDecimal("3.00"), BigDecimal.ZERO, 3, 0, 10, true);

        when(calculationService.calculateShortageProjection(studentId, subjectId, sectionId, policyId, periodId, 10))
            .thenReturn(response);

        mockMvc.perform(get("/api/v1/students/{studentId}/attendance/subjects/{subjectId}/shortage", studentId, subjectId)
                .param("sectionId", sectionId.toString())
                .param("policyId", policyId.toString())
                .param("periodId", periodId.toString())
                .param("projectedRemainingUnits", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.minimumAdditionalUnitsRequired").value(3))
            .andExpect(jsonPath("$.isPossibleToRecover").value(true));
    }
}
