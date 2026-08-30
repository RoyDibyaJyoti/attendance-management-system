package com.amcs.infrastructure.web.controller;

import com.amcs.application.dto.importer.ImportCommitResponse;
import com.amcs.application.dto.importer.ImportJobResponse;
import com.amcs.application.port.out.excel.ExcelWorkbookGeneratorPort;
import com.amcs.application.port.out.security.CurrentUserPort;
import com.amcs.application.security.ApplicationAuthorizationService;
import com.amcs.application.service.ImportApplicationService;
import com.amcs.application.service.importer.dto.ImportCommitResult;
import com.amcs.application.service.importer.dto.ImportSubmissionResult;
import com.amcs.domain.importer.ImportJob;
import com.amcs.domain.importer.ImportMode;
import com.amcs.domain.importer.ImportStatus;
import com.amcs.domain.importer.ImportType;
import com.amcs.domain.importer.RowValidationError;
import com.amcs.infrastructure.excel.adapter.ApachePoiExcelWorkbookGeneratorAdapter;
import com.amcs.infrastructure.excel.generator.StreamingExcelGenerator;
import com.amcs.infrastructure.web.error.GlobalRestExceptionHandler;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImportController API Behavior Tests")
class ImportControllerApiIntegrationTest {

    @Mock private ImportApplicationService importApplicationService;
    @Mock private ApplicationAuthorizationService authorizationService;

    private ExcelWorkbookGeneratorPort workbookGeneratorPort;
    private MockMvc mockMvc;

    private byte[] validStudentsXlsxBytes;
    private final UUID adminUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        StreamingExcelGenerator generator = new StreamingExcelGenerator();
        workbookGeneratorPort = new ApachePoiExcelWorkbookGeneratorAdapter(generator);
        validStudentsXlsxBytes = generator.generateTemplate(ImportType.STUDENTS);

        ImportController controller = new ImportController(
            importApplicationService,
            workbookGeneratorPort,
            authorizationService
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalRestExceptionHandler())
            .build();
    }

    @Nested
    @DisplayName("1. Template Downloads")
    class TemplateDownloads {

        @Test
        @DisplayName("GET /api/v1/imports/templates/STUDENTS returns 200 with XLSX attachment")
        void downloadStudentsTemplate() throws Exception {
            mockMvc.perform(get("/api/v1/imports/templates/STUDENTS"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"template-students.xlsx\""));
        }

        @Test
        @DisplayName("GET /api/v1/imports/templates/SESSIONS returns 200")
        void downloadSessionsTemplate() throws Exception {
            mockMvc.perform(get("/api/v1/imports/templates/SESSIONS"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"template-sessions.xlsx\""));
        }

        @Test
        @DisplayName("GET /api/v1/imports/templates/ATTENDANCE_RECORDS returns 200")
        void downloadAttendanceTemplate() throws Exception {
            mockMvc.perform(get("/api/v1/imports/templates/ATTENDANCE_RECORDS"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"template-attendance_records.xlsx\""));
        }

        @Test
        @DisplayName("GET /api/v1/imports/templates/INVALID_TYPE returns 400 Bad Request")
        void downloadInvalidType() throws Exception {
            mockMvc.perform(get("/api/v1/imports/templates/INVALID_TYPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        }
    }

    @Nested
    @DisplayName("2. Upload & Inspection Validation")
    class UploadAndInspectionValidation {

        @Test
        @DisplayName("Upload valid spreadsheet returns 202 Accepted")
        void uploadValidSpreadsheet() throws Exception {
            UUID jobId = UUID.randomUUID();
            ImportSubmissionResult result = new ImportSubmissionResult(
                jobId, ImportType.STUDENTS, ImportMode.FAIL_FAST, ImportStatus.STAGED_CLEAN, "students.xlsx", 5, 5, 0, List.of(), adminUserId, Instant.now(), null, null, true
            );
            when(importApplicationService.submitImport(any(), eq(ImportType.STUDENTS), eq(ImportMode.FAIL_FAST), any())).thenReturn(result);

            MockMultipartFile file = new MockMultipartFile("file", "students.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", validStudentsXlsxBytes);

            mockMvc.perform(multipart("/api/v1/imports/STUDENTS")
                    .file(file)
                    .param("mode", "FAIL_FAST"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.status").value("STAGED_CLEAN"))
                .andExpect(jsonPath("$.validRows").value(5))
                .andExpect(jsonPath("$.isCommittable").value(true));
        }

        @Test
        @DisplayName("Upload empty file returns 400 Bad Request")
        void uploadEmptyFile() throws Exception {
            MockMultipartFile file = new MockMultipartFile("file", "empty.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]);

            mockMvc.perform(multipart("/api/v1/imports/STUDENTS")
                    .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        }

        @Test
        @DisplayName("Upload non-XLSX plain text returns 422 Unprocessable Entity")
        void uploadNonXlsxPlainText() throws Exception {
            MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "This is not an XLSX file".getBytes(StandardCharsets.UTF_8));

            mockMvc.perform(multipart("/api/v1/imports/STUDENTS")
                    .file(file))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_ARCHIVE_MAGIC"));
        }

        @Test
        @DisplayName("Upload corrupted ZIP file returns 422 Unprocessable Entity")
        void uploadCorruptedZip() throws Exception {
            byte[] corruptedZip = new byte[]{0x50, 0x4B, 0x03, 0x04, 0x00, 0x00, 0x00, 0x00, 0x01, 0x02};
            MockMultipartFile file = new MockMultipartFile("file", "corrupt.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", corruptedZip);

            mockMvc.perform(multipart("/api/v1/imports/STUDENTS")
                    .file(file))
                .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("Upload ZIP with path traversal entry returns 422 Unprocessable Entity")
        void uploadZipWithPathTraversal() throws Exception {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                ZipEntry entry = new ZipEntry("../evil.txt");
                zos.putNextEntry(entry);
                zos.write("payload".getBytes());
                zos.closeEntry();
            }
            MockMultipartFile file = new MockMultipartFile("file", "traversal.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", baos.toByteArray());

            mockMvc.perform(multipart("/api/v1/imports/STUDENTS")
                    .file(file))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PATH_TRAVERSAL_DETECTED"));
        }

        @Test
        @DisplayName("Upload with invalid mode returns 400 Bad Request")
        void uploadInvalidMode() throws Exception {
            MockMultipartFile file = new MockMultipartFile("file", "students.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", validStudentsXlsxBytes);

            mockMvc.perform(multipart("/api/v1/imports/STUDENTS")
                    .file(file)
                    .param("mode", "UNKNOWN_MODE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        }

        @Test
        @DisplayName("Upload with unsupported import type returns 400 Bad Request")
        void uploadUnsupportedImportType() throws Exception {
            MockMultipartFile file = new MockMultipartFile("file", "students.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", validStudentsXlsxBytes);

            mockMvc.perform(multipart("/api/v1/imports/FACULTIES")
                    .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        }
    }

    @Nested
    @DisplayName("3. Job Status & Error Downloading")
    class JobStatusAndErrors {

        @Test
        @DisplayName("GET /api/v1/imports/jobs/{jobId} returns 200 on existing job")
        void getExistingJob() throws Exception {
            UUID jobId = UUID.randomUUID();
            ImportSubmissionResult result = new ImportSubmissionResult(
                jobId, ImportType.SESSIONS, ImportMode.PARTIAL_COMMIT, ImportStatus.STAGED_PARTIAL, "sessions.xlsx", 10, 8, 2,
                List.of(new RowValidationError(3, "Session Date", "2025-01-01", "DATE_OUTSIDE_ACADEMIC_PERIOD", "Date is out of bounds")),
                adminUserId, Instant.now(), null, null, true
            );
            when(importApplicationService.getJob(jobId)).thenReturn(result);

            mockMvc.perform(get("/api/v1/imports/jobs/" + jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.status").value("STAGED_PARTIAL"))
                .andExpect(jsonPath("$.validRows").value(8))
                .andExpect(jsonPath("$.invalidRows").value(2))
                .andExpect(jsonPath("$.errors[0].errorCode").value("DATE_OUTSIDE_ACADEMIC_PERIOD"));
        }

        @Test
        @DisplayName("GET /api/v1/imports/jobs/{jobId} returns 404 when job not found")
        void getNonExistentJob() throws Exception {
            UUID jobId = UUID.randomUUID();
            when(importApplicationService.getJob(jobId))
                .thenThrow(new com.amcs.application.exception.ResourceNotFoundException("Import job not found: " + jobId));

            mockMvc.perform(get("/api/v1/imports/jobs/" + jobId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }

        @Test
        @DisplayName("GET /api/v1/imports/jobs/{jobId}/errors/download returns 200 with escaped formula cells")
        void downloadErrorReport() throws Exception {
            UUID jobId = UUID.randomUUID();
            StreamingExcelGenerator generator = new StreamingExcelGenerator();
            List<RowValidationError> errors = List.of(
                new RowValidationError(2, "=1+1", "@calc.exe", "BAD_CODE", "-harmful formula")
            );
            byte[] errorXlsx = generator.generateErrorReport(errors);

            when(importApplicationService.getJobErrorReport(jobId)).thenReturn(errorXlsx);

            MvcResult mvcResult = mockMvc.perform(get("/api/v1/imports/jobs/" + jobId + "/errors/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"errors-" + jobId + ".xlsx\""))
                .andReturn();

            byte[] content = mvcResult.getResponse().getContentAsByteArray();
            try (InputStream is = new ByteArrayInputStream(content);
                 Workbook wb = WorkbookFactory.create(is)) {
                org.apache.poi.ss.usermodel.Sheet sheet = wb.getSheetAt(0);
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(1);
                // Verify FormulaEscaper prefix (')
                assertThat(row.getCell(1).getStringCellValue()).startsWith("'=");
                assertThat(row.getCell(2).getStringCellValue()).startsWith("'@");
                assertThat(row.getCell(4).getStringCellValue()).startsWith("'-");
            }
        }
    }

    @Nested
    @DisplayName("4. Commit and Discard Operations")
    class CommitAndDiscardOperations {

        @Test
        @DisplayName("POST /api/v1/imports/jobs/{jobId}/commit returns 200 on successful commit")
        void commitJobSuccessfully() throws Exception {
            UUID jobId = UUID.randomUUID();
            ImportCommitResult result = new ImportCommitResult(jobId, ImportStatus.COMMITTED, 10, Instant.now());
            when(importApplicationService.commitImport(jobId)).thenReturn(result);

            mockMvc.perform(post("/api/v1/imports/jobs/" + jobId + "/commit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.status").value("COMMITTED"))
                .andExpect(jsonPath("$.committedRows").value(10));
        }

        @Test
        @DisplayName("POST /api/v1/imports/jobs/{jobId}/commit returns 409 on lifecycle conflict (already committed)")
        void commitAlreadyCommittedJob() throws Exception {
            UUID jobId = UUID.randomUUID();
            when(importApplicationService.commitImport(jobId))
                .thenThrow(new IllegalStateException("Import job has already been committed at: " + Instant.now()));

            mockMvc.perform(post("/api/v1/imports/jobs/" + jobId + "/commit"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LIFECYCLE_CONFLICT"));
        }

        @Test
        @DisplayName("POST /api/v1/imports/jobs/{jobId}/discard returns 204 No Content on success")
        void discardJobSuccessfully() throws Exception {
            UUID jobId = UUID.randomUUID();
            ImportSubmissionResult result = new ImportSubmissionResult(
                jobId, ImportType.STUDENTS, ImportMode.FAIL_FAST, ImportStatus.DISCARDED, "students.xlsx", 5, 5, 0, List.of(), adminUserId, Instant.now(), null, Instant.now(), false
            );
            when(importApplicationService.discardImport(jobId)).thenReturn(result);

            mockMvc.perform(post("/api/v1/imports/jobs/" + jobId + "/discard"))
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("POST /api/v1/imports/jobs/{jobId}/discard returns 409 on already committed job")
        void discardAlreadyCommittedJob() throws Exception {
            UUID jobId = UUID.randomUUID();
            when(importApplicationService.discardImport(jobId))
                .thenThrow(new IllegalStateException("Cannot discard an already committed import job"));

            mockMvc.perform(post("/api/v1/imports/jobs/" + jobId + "/discard"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LIFECYCLE_CONFLICT"));
        }

        @Test
        @DisplayName("POST /api/v1/imports/jobs/{jobId}/commit returns 404 when job does not exist")
        void commitNonExistentJob() throws Exception {
            UUID jobId = UUID.randomUUID();
            when(importApplicationService.commitImport(jobId))
                .thenThrow(new com.amcs.application.exception.ResourceNotFoundException("Import job not found: " + jobId));

            mockMvc.perform(post("/api/v1/imports/jobs/" + jobId + "/commit"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }
    }
}
