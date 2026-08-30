package com.amcs.infrastructure.security.rbac;

import com.amcs.application.exception.AccessDeniedException;
import com.amcs.application.port.out.security.UserAccount;
import com.amcs.application.port.out.security.UserAccountRepositoryPort;
import com.amcs.application.port.out.security.UserAccountStatus;
import com.amcs.application.port.out.security.UserRole;
import com.amcs.application.service.AttendanceReportApplicationService;
import com.amcs.infrastructure.security.error.RestAuthenticationEntryPoint;
import com.amcs.infrastructure.security.jwt.JwtAuthenticationFilter;
import com.amcs.infrastructure.security.jwt.JwtProperties;
import com.amcs.infrastructure.security.jwt.JwtTokenProvider;
import com.amcs.infrastructure.web.controller.AttendanceReportController;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceReportController RBAC and Scope Security Tests")
class AttendanceReportControllerSecurityIntegrationTest {

    private static final String TEST_SECRET = "super-secret-key-that-is-at-least-256-bits-long-32-bytes!";
    private static final String ISSUER = "amcs-auth-service";

    @Mock private UserAccountRepositoryPort userAccountRepositoryPort;
    @Mock private AttendanceReportApplicationService reportApplicationService;

    private MockMvc mockMvc;
    private JwtTokenProvider tokenProvider;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private final UUID studentUserId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID otherStudentId = UUID.randomUUID();

    private final UUID facultyUserId = UUID.randomUUID();
    private final UUID facultyId = UUID.randomUUID();
    private final UUID otherFacultyId = UUID.randomUUID();

    private final UUID adminUserId = UUID.randomUUID();

    private final UUID subjectId = UUID.randomUUID();
    private final UUID sectionId = UUID.randomUUID();
    private final UUID academicPeriodId = UUID.randomUUID();

    private String studentToken;
    private String facultyToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        JwtProperties jwtProperties = new JwtProperties(TEST_SECRET, ISSUER, 3600L);
        tokenProvider = new JwtTokenProvider(jwtProperties, Clock.systemUTC());

        RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(objectMapper);
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(tokenProvider, userAccountRepositoryPort, entryPoint);

        AttendanceReportController controller = new AttendanceReportController(reportApplicationService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalRestExceptionHandler())
            .addFilter(jwtFilter)
            .build();

        studentToken = createToken(studentUserId, "student1", UserRole.STUDENT, Optional.of(studentId), Optional.empty());
        facultyToken = createToken(facultyUserId, "faculty1", UserRole.FACULTY, Optional.empty(), Optional.of(facultyId));
        adminToken = createToken(adminUserId, "admin1", UserRole.HOD_ADMIN, Optional.empty(), Optional.empty());

        mockUserAccount(studentUserId, "student1", UserRole.STUDENT, Optional.of(studentId), Optional.empty());
        mockUserAccount(facultyUserId, "faculty1", UserRole.FACULTY, Optional.empty(), Optional.of(facultyId));
        mockUserAccount(adminUserId, "admin1", UserRole.HOD_ADMIN, Optional.empty(), Optional.empty());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockUserAccount(UUID userId, String username, UserRole role, Optional<UUID> studentId, Optional<UUID> facultyId) {
        UserAccount account = new UserAccount(
            userId, username, username + "@univ.edu", "hash", role, studentId, facultyId,
            UserAccountStatus.ACTIVE, 0, Optional.empty(), 1, Instant.now(), Instant.now()
        );
        lenient().when(userAccountRepositoryPort.findById(userId)).thenReturn(Optional.of(account));
    }

    private String createToken(UUID userId, String username, UserRole role, Optional<UUID> studentId, Optional<UUID> facultyId) {
        com.amcs.application.dto.security.AuthenticationResult auth = new com.amcs.application.dto.security.AuthenticationResult(
            userId, username, username + "@univ.edu", role, studentId, facultyId, 1
        );
        return "Bearer " + tokenProvider.generateToken(auth);
    }

    @Nested
    @DisplayName("Unauthenticated Access Restrictions")
    class UnauthenticatedRestrictions {

        @Test
        @DisplayName("Invalid token request to any report returns 401 Unauthorized")
        void unauthenticatedReturns401() throws Exception {
            mockMvc.perform(get("/api/v1/reports/rpt-001")
                    .header("Authorization", "Bearer invalid-tampered-token")
                    .param("studentId", studentId.toString())
                    .param("academicPeriodId", academicPeriodId.toString()))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Student Role & Scope Restrictions")
    class StudentRestrictions {

        @Test
        @DisplayName("Student can download their own RPT-001 attendance report")
        void studentCanDownloadOwnRpt001() throws Exception {
            mockMvc.perform(get("/api/v1/reports/rpt-001")
                    .header("Authorization", studentToken)
                    .param("studentId", studentId.toString())
                    .param("academicPeriodId", academicPeriodId.toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        }

        @Test
        @DisplayName("Student cannot download another student's RPT-001 attendance report (IDOR blocked -> 403)")
        void studentCannotDownloadOtherStudentRpt001() throws Exception {
            doThrow(new AccessDeniedException("Access denied: Students may only access their own attendance report"))
                .when(reportApplicationService).verifyRpt001Access(eq(otherStudentId));

            mockMvc.perform(get("/api/v1/reports/rpt-001")
                    .header("Authorization", studentToken)
                    .param("studentId", otherStudentId.toString())
                    .param("academicPeriodId", academicPeriodId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }

        @Test
        @DisplayName("Student cannot access RPT-002 Subject Summary -> 403")
        void studentForbiddenFromRpt002() throws Exception {
            doThrow(new AccessDeniedException("Access denied: Students cannot access Subject Attendance Summary"))
                .when(reportApplicationService).verifySubjectSectionAccess(any(), any(), any(), eq("Subject Attendance Summary"));

            mockMvc.perform(get("/api/v1/reports/rpt-002")
                    .header("Authorization", studentToken)
                    .param("subjectId", subjectId.toString())
                    .param("sectionId", sectionId.toString())
                    .param("academicPeriodId", academicPeriodId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }

        @Test
        @DisplayName("Student cannot access RPT-003 Defaulter Report -> 403")
        void studentForbiddenFromRpt003() throws Exception {
            doThrow(new AccessDeniedException("Access denied: Students cannot access Defaulter Report"))
                .when(reportApplicationService).verifyDefaulterAccess(any(), any(), any());

            mockMvc.perform(get("/api/v1/reports/rpt-003")
                    .header("Authorization", studentToken)
                    .param("academicPeriodId", academicPeriodId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }

        @Test
        @DisplayName("Student cannot access RPT-004 Attendance Register -> 403")
        void studentForbiddenFromRpt004() throws Exception {
            doThrow(new AccessDeniedException("Access denied: Students cannot access Attendance Register"))
                .when(reportApplicationService).verifySubjectSectionAccess(any(), any(), any(), eq("Attendance Register"));

            mockMvc.perform(get("/api/v1/reports/rpt-004")
                    .header("Authorization", studentToken)
                    .param("subjectId", subjectId.toString())
                    .param("sectionId", sectionId.toString())
                    .param("academicPeriodId", academicPeriodId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }

        @Test
        @DisplayName("Student cannot access RPT-005 Overall Summary -> 403")
        void studentForbiddenFromRpt005() throws Exception {
            doThrow(new AccessDeniedException("Access denied: Students cannot access Overall Attendance Summary"))
                .when(reportApplicationService).verifyOverallAccess(any(), any());

            mockMvc.perform(get("/api/v1/reports/rpt-005")
                    .header("Authorization", studentToken)
                    .param("academicPeriodId", academicPeriodId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }

        @Test
        @DisplayName("Student cannot access RPT-006 Faculty Compliance -> 403")
        void studentForbiddenFromRpt006() throws Exception {
            doThrow(new AccessDeniedException("Access denied: Students cannot access faculty compliance reports"))
                .when(reportApplicationService).verifyFacultyComplianceAccess(any());

            mockMvc.perform(get("/api/v1/reports/rpt-006")
                    .header("Authorization", studentToken)
                    .param("academicPeriodId", academicPeriodId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }

        @Test
        @DisplayName("Student cannot access RPT-007 Condonation Register -> 403")
        void studentForbiddenFromRpt007() throws Exception {
            doThrow(new AccessDeniedException("Access denied: Only administrators may access condonation register"))
                .when(reportApplicationService).verifyCondonationAccess(any(), any(), any());

            mockMvc.perform(get("/api/v1/reports/rpt-007")
                    .header("Authorization", studentToken)
                    .param("academicPeriodId", academicPeriodId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }

        @Test
        @DisplayName("Student cannot access RPT-008 Prediction Report -> 403")
        void studentForbiddenFromRpt008() throws Exception {
            doThrow(new AccessDeniedException("Access denied: Students cannot access Prediction Report"))
                .when(reportApplicationService).verifySubjectSectionAccess(any(), any(), any(), eq("Prediction Report"));

            mockMvc.perform(get("/api/v1/reports/rpt-008")
                    .header("Authorization", studentToken)
                    .param("subjectId", subjectId.toString())
                    .param("sectionId", sectionId.toString())
                    .param("academicPeriodId", academicPeriodId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }
    }

    @Nested
    @DisplayName("Faculty Scope Restrictions")
    class FacultyRestrictions {

        @Test
        @DisplayName("Faculty is forbidden from accessing reports for unassigned courses/sections -> 403")
        void facultyForbiddenFromUnassignedSubject() throws Exception {
            doThrow(new AccessDeniedException("Access denied: Faculty member is not assigned to this subject and section"))
                .when(reportApplicationService).verifySubjectSectionAccess(any(), any(), any(), any());

            mockMvc.perform(get("/api/v1/reports/rpt-002")
                    .header("Authorization", facultyToken)
                    .param("subjectId", subjectId.toString())
                    .param("sectionId", sectionId.toString())
                    .param("academicPeriodId", academicPeriodId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }

        @Test
        @DisplayName("Faculty is allowed to access reports for assigned courses/sections -> 200")
        void facultyAllowedForAssignedSubject() throws Exception {
            mockMvc.perform(get("/api/v1/reports/rpt-002")
                    .header("Authorization", facultyToken)
                    .param("subjectId", subjectId.toString())
                    .param("sectionId", sectionId.toString())
                    .param("academicPeriodId", academicPeriodId.toString()))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Faculty is forbidden from inspecting other faculty's marking compliance (IDOR) -> 403")
        void facultyForbiddenFromOtherFacultyCompliance() throws Exception {
            doThrow(new AccessDeniedException("Access denied: Faculty members cannot view compliance reports of other faculty"))
                .when(reportApplicationService).verifyFacultyComplianceAccess(eq(otherFacultyId));

            mockMvc.perform(get("/api/v1/reports/rpt-006")
                    .header("Authorization", facultyToken)
                    .param("academicPeriodId", academicPeriodId.toString())
                    .param("facultyId", otherFacultyId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }

        @Test
        @DisplayName("Faculty can inspect their own marking compliance report -> 200")
        void facultyAllowedOwnCompliance() throws Exception {
            mockMvc.perform(get("/api/v1/reports/rpt-006")
                    .header("Authorization", facultyToken)
                    .param("academicPeriodId", academicPeriodId.toString())
                    .param("facultyId", facultyId.toString()))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("HOD_ADMIN Unrestricted Access")
    class AdminPermissions {

        @Test
        @DisplayName("HOD_ADMIN can access all reports without restriction -> 200")
        void adminCanAccessAnyReport() throws Exception {
            mockMvc.perform(get("/api/v1/reports/rpt-001")
                    .header("Authorization", adminToken)
                    .param("studentId", studentId.toString())
                    .param("academicPeriodId", academicPeriodId.toString()))
                .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/reports/rpt-006")
                    .header("Authorization", adminToken)
                    .param("academicPeriodId", academicPeriodId.toString())
                    .param("facultyId", otherFacultyId.toString()))
                .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/reports/rpt-007")
                    .header("Authorization", adminToken)
                    .param("academicPeriodId", academicPeriodId.toString()))
                .andExpect(status().isOk());
        }
    }
}
