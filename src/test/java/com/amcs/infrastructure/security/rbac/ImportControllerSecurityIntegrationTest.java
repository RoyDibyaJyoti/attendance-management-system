package com.amcs.infrastructure.security.rbac;

import com.amcs.application.port.out.excel.ExcelWorkbookGeneratorPort;
import com.amcs.application.port.out.security.CurrentUserPort;
import com.amcs.application.port.out.security.FacultyAssignmentRepositoryPort;
import com.amcs.application.port.out.security.UserAccount;
import com.amcs.application.port.out.security.UserAccountRepositoryPort;
import com.amcs.application.port.out.security.UserAccountStatus;
import com.amcs.application.port.out.security.UserRole;
import com.amcs.application.security.ApplicationAuthorizationService;
import com.amcs.application.service.ImportApplicationService;
import com.amcs.application.service.importer.dto.ImportCommitResult;
import com.amcs.application.service.importer.dto.ImportSubmissionResult;
import com.amcs.domain.importer.ImportJob;
import com.amcs.domain.importer.ImportMode;
import com.amcs.domain.importer.ImportStatus;
import com.amcs.domain.importer.ImportType;
import com.amcs.domain.importer.RowValidationError;
import com.amcs.infrastructure.excel.generator.StreamingExcelGenerator;
import com.amcs.infrastructure.security.adapter.SpringSecurityCurrentUserAdapter;
import com.amcs.infrastructure.security.error.RestAuthenticationEntryPoint;
import com.amcs.infrastructure.security.jwt.JwtAuthenticationFilter;
import com.amcs.infrastructure.security.jwt.JwtProperties;
import com.amcs.infrastructure.security.jwt.JwtTokenProvider;
import com.amcs.infrastructure.web.controller.ImportController;
import com.amcs.infrastructure.web.error.GlobalRestExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImportController RBAC and IDOR Security Tests")
class ImportControllerSecurityIntegrationTest {

    private static final String TEST_SECRET = "super-secret-key-that-is-at-least-256-bits-long-32-bytes!";
    private static final String ISSUER = "amcs-auth-service";

    @Mock private UserAccountRepositoryPort userAccountRepositoryPort;
    @Mock private FacultyAssignmentRepositoryPort facultyAssignmentPort;
    @Mock private ImportApplicationService importApplicationService;

    private ExcelWorkbookGeneratorPort workbookGeneratorPort;
    private MockMvc mockMvc;
    private JwtTokenProvider tokenProvider;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private final UUID studentUserId = UUID.randomUUID();
    private final UUID facultyUserId = UUID.randomUUID();
    private final UUID otherFacultyUserId = UUID.randomUUID();
    private final UUID adminUserId = UUID.randomUUID();

    private String studentToken;
    private String facultyToken;
    private String otherFacultyToken;
    private String adminToken;

    private byte[] validXlsxBytes;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        JwtProperties jwtProperties = new JwtProperties(TEST_SECRET, ISSUER, 3600L);
        tokenProvider = new JwtTokenProvider(jwtProperties, Clock.systemUTC());

        RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(objectMapper);
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(tokenProvider, userAccountRepositoryPort, entryPoint);

        CurrentUserPort currentUserPort = new SpringSecurityCurrentUserAdapter();
        ApplicationAuthorizationService authService = new ApplicationAuthorizationService(currentUserPort, facultyAssignmentPort);

        StreamingExcelGenerator generator = new StreamingExcelGenerator();
        workbookGeneratorPort = new com.amcs.infrastructure.excel.adapter.ApachePoiExcelWorkbookGeneratorAdapter(generator);

        validXlsxBytes = generator.generateTemplate(ImportType.STUDENTS);

        ImportController controller = new ImportController(
            importApplicationService,
            workbookGeneratorPort,
            authService
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalRestExceptionHandler())
            .addFilter(jwtFilter)
            .build();

        studentToken = createToken(studentUserId, "student1", UserRole.STUDENT, Optional.of(UUID.randomUUID()), Optional.empty(), 1);
        facultyToken = createToken(facultyUserId, "faculty1", UserRole.FACULTY, Optional.empty(), Optional.of(UUID.randomUUID()), 1);
        otherFacultyToken = createToken(otherFacultyUserId, "faculty2", UserRole.FACULTY, Optional.empty(), Optional.of(UUID.randomUUID()), 1);
        adminToken = createToken(adminUserId, "admin1", UserRole.HOD_ADMIN, Optional.empty(), Optional.empty(), 1);

        mockUserAccount(studentUserId, "student1", UserRole.STUDENT, Optional.of(UUID.randomUUID()), Optional.empty(), 1);
        mockUserAccount(facultyUserId, "faculty1", UserRole.FACULTY, Optional.empty(), Optional.of(UUID.randomUUID()), 1);
        mockUserAccount(otherFacultyUserId, "faculty2", UserRole.FACULTY, Optional.empty(), Optional.of(UUID.randomUUID()), 1);
        mockUserAccount(adminUserId, "admin1", UserRole.HOD_ADMIN, Optional.empty(), Optional.empty(), 1);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockUserAccount(UUID userId, String username, UserRole role, Optional<UUID> studentId, Optional<UUID> facultyId, int version) {
        UserAccount account = new UserAccount(
            userId, username, username + "@univ.edu", "hash", role, studentId, facultyId,
            UserAccountStatus.ACTIVE, 0, Optional.empty(), version, Instant.now(), Instant.now()
        );
        org.mockito.Mockito.lenient().when(userAccountRepositoryPort.findById(userId)).thenReturn(Optional.of(account));
    }

    private String createToken(UUID userId, String username, UserRole role, Optional<UUID> studentId, Optional<UUID> facultyId, int version) {
        com.amcs.application.dto.security.AuthenticationResult auth = new com.amcs.application.dto.security.AuthenticationResult(
            userId, username, username + "@univ.edu", role, studentId, facultyId, version
        );
        return "Bearer " + tokenProvider.generateToken(auth);
    }

    @Nested
    @DisplayName("Student Role Restrictions")
    class StudentRestrictions {

        @Test
        @DisplayName("Student cannot download any import template -> 403")
        void studentCannotDownloadTemplate() throws Exception {
            mockMvc.perform(get("/api/v1/imports/templates/STUDENTS")
                    .header("Authorization", studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }

        @Test
        @DisplayName("Student cannot upload STUDENTS -> 403")
        void studentCannotUploadStudents() throws Exception {
            MockMultipartFile file = new MockMultipartFile("file", "students.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", validXlsxBytes);
            when(importApplicationService.submitImport(any(), eq(ImportType.STUDENTS), any(), any()))
                .thenThrow(new com.amcs.application.exception.AccessDeniedException("Access denied: Only HOD_ADMIN can import student rosters"));

            mockMvc.perform(multipart("/api/v1/imports/STUDENTS")
                    .file(file)
                    .header("Authorization", studentToken))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Student cannot upload SESSIONS -> 403")
        void studentCannotUploadSessions() throws Exception {
            MockMultipartFile file = new MockMultipartFile("file", "sessions.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", validXlsxBytes);
            when(importApplicationService.submitImport(any(), eq(ImportType.SESSIONS), any(), any()))
                .thenThrow(new com.amcs.application.exception.AccessDeniedException("Access denied: Students are not permitted to submit imports"));

            mockMvc.perform(multipart("/api/v1/imports/SESSIONS")
                    .file(file)
                    .header("Authorization", studentToken))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Student cannot access another user's import job -> 403")
        void studentCannotAccessAnotherUsersJob() throws Exception {
            UUID jobId = UUID.randomUUID();
            when(importApplicationService.getJob(jobId))
                .thenThrow(new com.amcs.application.exception.AccessDeniedException("Access denied: You cannot view an import job submitted by another user"));

            mockMvc.perform(get("/api/v1/imports/jobs/" + jobId)
                    .header("Authorization", studentToken))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Student cannot commit another user's job -> 403")
        void studentCannotCommitJob() throws Exception {
            UUID jobId = UUID.randomUUID();
            when(importApplicationService.commitImport(jobId))
                .thenThrow(new com.amcs.application.exception.AccessDeniedException("Access denied: You cannot commit an import job submitted by another user"));

            mockMvc.perform(post("/api/v1/imports/jobs/" + jobId + "/commit")
                    .header("Authorization", studentToken))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Student cannot discard another user's job -> 403")
        void studentCannotDiscardJob() throws Exception {
            UUID jobId = UUID.randomUUID();
            when(importApplicationService.discardImport(jobId))
                .thenThrow(new com.amcs.application.exception.AccessDeniedException("Access denied: You cannot discard an import job submitted by another user"));

            mockMvc.perform(post("/api/v1/imports/jobs/" + jobId + "/discard")
                    .header("Authorization", studentToken))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Faculty Role Permissions & Scope Restrictions")
    class FacultyPermissionsAndScopes {

        @Test
        @DisplayName("Faculty can download templates -> 200")
        void facultyCanDownloadTemplates() throws Exception {
            mockMvc.perform(get("/api/v1/imports/templates/SESSIONS")
                    .header("Authorization", facultyToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"template-sessions.xlsx\""));
        }

        @Test
        @DisplayName("Faculty cannot import STUDENTS -> 403")
        void facultyCannotImportStudents() throws Exception {
            MockMultipartFile file = new MockMultipartFile("file", "students.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", validXlsxBytes);
            when(importApplicationService.submitImport(any(), eq(ImportType.STUDENTS), any(), any()))
                .thenThrow(new com.amcs.application.exception.AccessDeniedException("Access denied: Only HOD_ADMIN can import student rosters"));

            mockMvc.perform(multipart("/api/v1/imports/STUDENTS")
                    .file(file)
                    .header("Authorization", facultyToken))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Faculty can import own authorized SESSIONS -> 202")
        void facultyCanImportAuthorizedSessions() throws Exception {
            MockMultipartFile file = new MockMultipartFile("file", "sessions.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", validXlsxBytes);
            UUID jobId = UUID.randomUUID();
            ImportSubmissionResult result = new ImportSubmissionResult(
                jobId, ImportType.SESSIONS, ImportMode.FAIL_FAST, ImportStatus.STAGED_CLEAN, "sessions.xlsx", 5, 5, 0, List.of(), facultyUserId, Instant.now(), null, null, true
            );
            when(importApplicationService.submitImport(any(), eq(ImportType.SESSIONS), any(), any())).thenReturn(result);

            mockMvc.perform(multipart("/api/v1/imports/SESSIONS")
                    .file(file)
                    .header("Authorization", facultyToken))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.status").value("STAGED_CLEAN"));
        }

        @Test
        @DisplayName("Faculty can access own job -> 200")
        void facultyCanAccessOwnJob() throws Exception {
            UUID jobId = UUID.randomUUID();
            ImportSubmissionResult result = new ImportSubmissionResult(
                jobId, ImportType.SESSIONS, ImportMode.FAIL_FAST, ImportStatus.STAGED_CLEAN, "sessions.xlsx", 5, 5, 0, List.of(), facultyUserId, Instant.now(), null, null, true
            );
            when(importApplicationService.getJob(jobId)).thenReturn(result);

            mockMvc.perform(get("/api/v1/imports/jobs/" + jobId)
                    .header("Authorization", facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId.toString()));
        }

        @Test
        @DisplayName("Faculty cannot access another faculty's job -> 403 (IDOR)")
        void facultyCannotAccessAnotherFacultysJob() throws Exception {
            UUID jobId = UUID.randomUUID();
            when(importApplicationService.getJob(jobId))
                .thenThrow(new com.amcs.application.exception.AccessDeniedException("Access denied: You cannot view an import job submitted by another user"));

            mockMvc.perform(get("/api/v1/imports/jobs/" + jobId)
                    .header("Authorization", otherFacultyToken))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Faculty cannot commit another faculty's job -> 403 (IDOR)")
        void facultyCannotCommitAnotherFacultysJob() throws Exception {
            UUID jobId = UUID.randomUUID();
            when(importApplicationService.commitImport(jobId))
                .thenThrow(new com.amcs.application.exception.AccessDeniedException("Access denied: You cannot commit an import job submitted by another user"));

            mockMvc.perform(post("/api/v1/imports/jobs/" + jobId + "/commit")
                    .header("Authorization", otherFacultyToken))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Faculty cannot discard another faculty's job -> 403 (IDOR)")
        void facultyCannotDiscardAnotherFacultysJob() throws Exception {
            UUID jobId = UUID.randomUUID();
            when(importApplicationService.discardImport(jobId))
                .thenThrow(new com.amcs.application.exception.AccessDeniedException("Access denied: You cannot discard an import job submitted by another user"));

            mockMvc.perform(post("/api/v1/imports/jobs/" + jobId + "/discard")
                    .header("Authorization", otherFacultyToken))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Faculty cannot download error report of another faculty's job -> 403 (IDOR)")
        void facultyCannotDownloadAnotherFacultysErrorReport() throws Exception {
            UUID jobId = UUID.randomUUID();
            when(importApplicationService.getJobErrorReport(jobId))
                .thenThrow(new com.amcs.application.exception.AccessDeniedException("Access denied: You cannot download error report for an import job submitted by another user"));

            mockMvc.perform(get("/api/v1/imports/jobs/" + jobId + "/errors/download")
                    .header("Authorization", otherFacultyToken))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("HOD_ADMIN Role Unrestricted Access")
    class AdminPermissions {

        @Test
        @DisplayName("HOD can download templates -> 200")
        void adminCanDownloadTemplates() throws Exception {
            mockMvc.perform(get("/api/v1/imports/templates/STUDENTS")
                    .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"template-students.xlsx\""));
        }

        @Test
        @DisplayName("HOD can submit STUDENTS import -> 202")
        void adminCanSubmitStudentsImport() throws Exception {
            MockMultipartFile file = new MockMultipartFile("file", "students.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", validXlsxBytes);
            UUID jobId = UUID.randomUUID();
            ImportSubmissionResult result = new ImportSubmissionResult(
                jobId, ImportType.STUDENTS, ImportMode.FAIL_FAST, ImportStatus.STAGED_CLEAN, "students.xlsx", 10, 10, 0, List.of(), adminUserId, Instant.now(), null, null, true
            );
            when(importApplicationService.submitImport(any(), eq(ImportType.STUDENTS), any(), any())).thenReturn(result);

            mockMvc.perform(multipart("/api/v1/imports/STUDENTS")
                    .file(file)
                    .header("Authorization", adminToken))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value(jobId.toString()));
        }

        @Test
        @DisplayName("HOD can inspect any user's job -> 200")
        void adminCanInspectAnyJob() throws Exception {
            UUID jobId = UUID.randomUUID();
            ImportSubmissionResult result = new ImportSubmissionResult(
                jobId, ImportType.SESSIONS, ImportMode.FAIL_FAST, ImportStatus.STAGED_CLEAN, "sessions.xlsx", 5, 5, 0, List.of(), facultyUserId, Instant.now(), null, null, true
            );
            when(importApplicationService.getJob(jobId)).thenReturn(result);

            mockMvc.perform(get("/api/v1/imports/jobs/" + jobId)
                    .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId.toString()));
        }

        @Test
        @DisplayName("HOD can commit permitted job -> 200")
        void adminCanCommitPermittedJob() throws Exception {
            UUID jobId = UUID.randomUUID();
            ImportCommitResult result = new ImportCommitResult(jobId, ImportStatus.COMMITTED, 5, Instant.now());
            when(importApplicationService.commitImport(jobId)).thenReturn(result);

            mockMvc.perform(post("/api/v1/imports/jobs/" + jobId + "/commit")
                    .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMMITTED"))
                .andExpect(jsonPath("$.committedRows").value(5));
        }

        @Test
        @DisplayName("HOD can discard permitted job -> 204")
        void adminCanDiscardPermittedJob() throws Exception {
            UUID jobId = UUID.randomUUID();
            mockMvc.perform(post("/api/v1/imports/jobs/" + jobId + "/discard")
                    .header("Authorization", adminToken))
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("HOD can download error reports -> 200")
        void adminCanDownloadErrorReport() throws Exception {
            UUID jobId = UUID.randomUUID();
            when(importApplicationService.getJobErrorReport(jobId)).thenReturn(new byte[]{1, 2, 3});

            mockMvc.perform(get("/api/v1/imports/jobs/" + jobId + "/errors/download")
                    .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"errors-" + jobId + ".xlsx\""));
        }
    }
}
