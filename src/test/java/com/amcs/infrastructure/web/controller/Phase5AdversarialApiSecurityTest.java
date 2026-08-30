package com.amcs.infrastructure.web.controller;

import com.amcs.application.exception.AccessDeniedException;
import com.amcs.application.exception.ResourceNotFoundException;
import com.amcs.application.service.AttendanceReportApplicationService;
import com.amcs.application.service.ImportApplicationService;
import com.amcs.infrastructure.web.error.GlobalRestExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 5.7: HTTP API Adversarial & Security Error Invariant Tests")
class Phase5AdversarialApiSecurityTest {

    @Mock private ImportApplicationService importService;
    @Mock private AttendanceReportApplicationService reportService;
    @Mock private com.amcs.application.port.out.excel.ExcelWorkbookGeneratorPort workbookGeneratorPort;
    @Mock private com.amcs.application.security.ApplicationAuthorizationService authorizationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ImportController importController = new ImportController(importService, workbookGeneratorPort, authorizationService);
        AttendanceReportController reportController = new AttendanceReportController(reportService);

        mockMvc = MockMvcBuilders.standaloneSetup(importController, reportController)
            .setControllerAdvice(new GlobalRestExceptionHandler())
            .build();
    }

    @Nested
    @DisplayName("G1: HTTP Input Validation & Malformed Payloads")
    class InputValidationTests {

        @Test
        @DisplayName("Rejects malformed non-UUID path parameter with 400 Bad Request")
        void rejectsMalformedUuidPath() throws Exception {
            mockMvc.perform(post("/api/v1/imports/jobs/not-a-valid-uuid/commit"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT_TYPE"));
        }

        @Test
        @DisplayName("Rejects invalid import type enum with 400 Bad Request")
        void rejectsInvalidImportTypeEnum() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                "file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "content".getBytes()
            );

            mockMvc.perform(multipart("/api/v1/imports/NON_EXISTENT_TYPE")
                    .file(file)
                    .param("mode", "FAIL_FAST"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        }

        @Test
        @DisplayName("Rejects missing required query parameter on report endpoints with 400 Bad Request")
        void rejectsMissingRequiredQueryParams() throws Exception {
            mockMvc.perform(get("/api/v1/reports/rpt-001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_PARAMETER"));
        }

        @Test
        @DisplayName("Rejects malformed date format in query parameter with 400 Bad Request")
        void rejectsMalformedDateQueryParam() throws Exception {
            mockMvc.perform(get("/api/v1/reports/rpt-004")
                    .param("subjectId", UUID.randomUUID().toString())
                    .param("sectionId", UUID.randomUUID().toString())
                    .param("academicPeriodId", UUID.randomUUID().toString())
                    .param("startDate", "invalid-date-format"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT_TYPE"));
        }
    }

    @Nested
    @DisplayName("G2: Information Leakage & Error Body Sanitization")
    class ErrorSanitizationTests {

        @Test
        @DisplayName("Error JSON does not expose internal stack traces, class names, or SQL fragments")
        void errorResponseDoesNotLeakInternals() throws Exception {
            UUID testJobId = UUID.randomUUID();
            doThrow(new ResourceNotFoundException("Import job not found: " + testJobId))
                .when(importService).commitImport(eq(testJobId));

            MvcResult result = mockMvc.perform(post("/api/v1/imports/jobs/" + testJobId + "/commit"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Import job not found: " + testJobId))
                .andReturn();

            String responseBody = result.getResponse().getContentAsString();

            // Strict leak assertions
            assertThat(responseBody).doesNotContain("Exception");
            assertThat(responseBody).doesNotContain("at com.amcs");
            assertThat(responseBody).doesNotContain("org.springframework");
            assertThat(responseBody).doesNotContain("SELECT");
            assertThat(responseBody).doesNotContain("INSERT");
            assertThat(responseBody).doesNotContain("UPDATE");
            assertThat(responseBody).doesNotContain("/Users/");
            assertThat(responseBody).doesNotContain("C:\\");
        }

        @Test
        @DisplayName("IDOR attempt maps to clean 403 ACCESS_DENIED without leaking internal actor state")
        void idorAttemptReturnsClean403() throws Exception {
            UUID victimStudentId = UUID.randomUUID();
            doThrow(new AccessDeniedException("Access denied: Students may only access their own attendance report"))
                .when(reportService).verifyRpt001Access(eq(victimStudentId));

            MvcResult result = mockMvc.perform(get("/api/v1/reports/rpt-001")
                    .param("studentId", victimStudentId.toString())
                    .param("academicPeriodId", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            assertThat(responseBody).doesNotContain("Exception");
            assertThat(responseBody).doesNotContain("at com.amcs");
        }
    }
}
