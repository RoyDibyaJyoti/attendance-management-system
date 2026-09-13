package com.amcs.infrastructure.security.error;

import com.amcs.application.dto.attendance.CorrectAttendanceRecordRequest;
import com.amcs.application.dto.attendance.RecordAttendanceBatchRequest;
import com.amcs.application.dto.security.AuthenticationResult;
import com.amcs.application.dto.session.CancelSessionRequest;
import com.amcs.application.dto.session.CreateSessionRequest;
import com.amcs.application.dto.student.UpdateStudentRequest;
import com.amcs.application.exception.UnauthenticatedException;
import com.amcs.application.port.out.AcademicPeriodRepositoryPort;
import com.amcs.application.port.out.AttendancePolicyRepositoryPort;
import com.amcs.application.port.out.AttendanceRecordRepositoryPort;
import com.amcs.application.port.out.DepartmentRepositoryPort;
import com.amcs.application.port.out.EnrollmentRepositoryPort;
import com.amcs.application.port.out.FacultyRepositoryPort;
import com.amcs.application.port.out.LabGroupRepositoryPort;
import com.amcs.application.port.out.OverallAttendancePolicyRepositoryPort;
import com.amcs.application.port.out.SectionRepositoryPort;
import com.amcs.application.port.out.SessionRepositoryPort;
import com.amcs.application.port.out.StudentRepositoryPort;
import com.amcs.application.port.out.SubjectRepositoryPort;
import com.amcs.application.port.out.security.CurrentUserPort;
import com.amcs.application.port.out.security.FacultyAssignmentRepositoryPort;
import com.amcs.application.port.out.security.UserAccount;
import com.amcs.application.port.out.security.UserAccountRepositoryPort;
import com.amcs.application.port.out.security.UserAccountStatus;
import com.amcs.application.port.out.security.UserRole;
import com.amcs.application.security.ApplicationAuthorizationService;
import com.amcs.application.service.AttendanceCalculationApplicationService;
import com.amcs.application.service.AttendanceRecordingApplicationService;
import com.amcs.application.service.SessionApplicationService;
import com.amcs.application.service.StudentApplicationService;
import com.amcs.domain.academic.AcademicPeriod;
import com.amcs.domain.academic.Subject;
import com.amcs.domain.attendance.AttendanceRecord;
import com.amcs.domain.attendance.AttendanceStatus;
import com.amcs.domain.attendance.Session;
import com.amcs.domain.attendance.SessionStatus;
import com.amcs.domain.attendance.SessionType;
import com.amcs.domain.policy.AttendancePolicy;
import com.amcs.domain.policy.MissingRecordStrategy;
import com.amcs.infrastructure.persistence.entity.StudentEntity;
import com.amcs.infrastructure.security.adapter.SpringSecurityCurrentUserAdapter;
import com.amcs.infrastructure.security.jwt.JwtAuthenticationFilter;
import com.amcs.infrastructure.security.jwt.JwtProperties;
import com.amcs.infrastructure.security.jwt.JwtTokenProvider;
import com.amcs.infrastructure.web.controller.AttendanceCalculationController;
import com.amcs.infrastructure.web.controller.AttendanceController;
import com.amcs.infrastructure.web.controller.SessionController;
import com.amcs.infrastructure.web.controller.StudentController;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SecurityErrorHandlingIntegrationTest {

    private static final String TEST_SECRET = "super-secret-key-that-is-at-least-256-bits-long-32-bytes!";
    private static final String WRONG_SECRET = "completely-wrong-secret-key-that-is-also-at-least-32-bytes!!";
    private static final String ISSUER = "amcs-auth-service";

    @Mock private UserAccountRepositoryPort userAccountRepositoryPort;
    @Mock private FacultyAssignmentRepositoryPort facultyAssignmentPort;
    @Mock private StudentRepositoryPort studentPort;
    @Mock private DepartmentRepositoryPort departmentPort;
    @Mock private SubjectRepositoryPort subjectPort;
    @Mock private SectionRepositoryPort sectionPort;
    @Mock private FacultyRepositoryPort facultyPort;
    @Mock private AcademicPeriodRepositoryPort periodPort;
    @Mock private SessionRepositoryPort sessionPort;
    @Mock private AttendanceRecordRepositoryPort recordPort;
    @Mock private EnrollmentRepositoryPort enrollmentPort;
    @Mock private LabGroupRepositoryPort labGroupPort;
    @Mock private AttendancePolicyRepositoryPort policyPort;
    @Mock private OverallAttendancePolicyRepositoryPort overallPolicyPort;

    private MockMvc studentMockMvc;
    private MockMvc sessionMockMvc;
    private MockMvc attendanceMockMvc;
    private MockMvc calculationMockMvc;

    private JwtTokenProvider tokenProvider;
    private JwtProperties jwtProperties;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private final UUID studentAId = UUID.randomUUID();
    private final UUID studentBId = UUID.randomUUID();
    private final UUID userAId = UUID.randomUUID();
    private final UUID userBId = UUID.randomUUID();
    private final UUID facultyAId = UUID.randomUUID();
    private final UUID facultyBId = UUID.randomUUID();
    private final UUID userFacultyAId = UUID.randomUUID();
    private final UUID userFacultyBId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID userAdminId = UUID.randomUUID();

    private final UUID deptId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();
    private final UUID sectionId = UUID.randomUUID();
    private final UUID periodId = UUID.randomUUID();
    private final UUID policyId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID recordId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtProperties = new JwtProperties(TEST_SECRET, ISSUER, 3600L);
        tokenProvider = new JwtTokenProvider(jwtProperties, Clock.systemUTC());

        RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(objectMapper);
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(tokenProvider, userAccountRepositoryPort, entryPoint);

        CurrentUserPort currentUserPort = new SpringSecurityCurrentUserAdapter();
        ApplicationAuthorizationService authService = new ApplicationAuthorizationService(currentUserPort, facultyAssignmentPort);

        StudentApplicationService studentService = new StudentApplicationService(studentPort, departmentPort, authService);
        SessionApplicationService sessionService = new SessionApplicationService(sessionPort, subjectPort, sectionPort, facultyPort, periodPort, authService);
        AttendanceRecordingApplicationService recordingService = new AttendanceRecordingApplicationService(sessionPort, recordPort, enrollmentPort, labGroupPort, subjectPort, periodPort, authService);
        AttendanceCalculationApplicationService calculationService = new AttendanceCalculationApplicationService(studentPort, subjectPort, sessionPort, recordPort, enrollmentPort, policyPort, overallPolicyPort, periodPort, authService);

        GlobalRestExceptionHandler exceptionHandler = new GlobalRestExceptionHandler();

        studentMockMvc = MockMvcBuilders.standaloneSetup(new StudentController(studentService))
            .setControllerAdvice(exceptionHandler)
            .addFilter(jwtFilter)
            .build();

        sessionMockMvc = MockMvcBuilders.standaloneSetup(new SessionController(sessionService))
            .setControllerAdvice(exceptionHandler)
            .addFilter(jwtFilter)
            .build();

        attendanceMockMvc = MockMvcBuilders.standaloneSetup(new AttendanceController(recordingService))
            .setControllerAdvice(exceptionHandler)
            .addFilter(jwtFilter)
            .build();

        calculationMockMvc = MockMvcBuilders.standaloneSetup(new AttendanceCalculationController(calculationService))
            .setControllerAdvice(exceptionHandler)
            .addFilter(jwtFilter)
            .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private String createToken(UUID userId, String username, UserRole role, Optional<UUID> studentId, Optional<UUID> facultyId, int version) {
        AuthenticationResult auth = new AuthenticationResult(userId, username, username + "@univ.edu", role, studentId, facultyId, version);
        return tokenProvider.generateToken(auth);
    }

    private UserAccount mockActiveAccount(UUID userId, String username, UserRole role, Optional<UUID> studentId, Optional<UUID> facultyId, int version) {
        UserAccount account = new UserAccount(
            userId, username, username + "@univ.edu", "hash", role, studentId, facultyId,
            UserAccountStatus.ACTIVE, 0, Optional.empty(), version, Instant.now(), Instant.now()
        );
        when(userAccountRepositoryPort.findById(userId)).thenReturn(Optional.of(account));
        return account;
    }

    @Nested
    @DisplayName("401 Unauthorized Scenarios & Envelope Verification")
    class UnauthorizedScenarios {

        @Test
        @DisplayName("1. Malformed Bearer token returns 401 MALFORMED_TOKEN in ApiErrorResponse")
        void shouldReturn401OnMalformedBearerToken() throws Exception {
            studentMockMvc.perform(get("/api/v1/students/" + studentAId)
                    .header("Authorization", "Bearer not.a.valid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("MALFORMED_TOKEN"))
                .andExpect(jsonPath("$.message").value("JWT token structure is malformed"))
                .andExpect(jsonPath("$.path").value("/api/v1/students/" + studentAId))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()))
                .andExpect(jsonPath("$.details", empty()));
        }

        @Test
        @DisplayName("2. Expired JWT returns 401 TOKEN_EXPIRED")
        void shouldReturn401OnExpiredToken() throws Exception {
            Clock pastClock = Clock.fixed(Instant.parse("2026-08-29T10:00:00Z"), ZoneOffset.UTC);
            JwtTokenProvider pastProvider = new JwtTokenProvider(jwtProperties, pastClock);
            AuthenticationResult auth = new AuthenticationResult(userAId, "studentA", "a@univ.edu", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);
            String expiredToken = pastProvider.generateToken(auth);

            studentMockMvc.perform(get("/api/v1/students/" + studentAId)
                    .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"))
                .andExpect(jsonPath("$.message").value("JWT token has expired"))
                .andExpect(jsonPath("$.path").value("/api/v1/students/" + studentAId));
        }

        @Test
        @DisplayName("3. Tampered JWT signature returns 401 INVALID_TOKEN")
        void shouldReturn401OnTamperedToken() throws Exception {
            String validToken = createToken(userAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);
            String tamperedToken = validToken.substring(0, validToken.length() - 8) + "xxxxxxxx";

            studentMockMvc.perform(get("/api/v1/students/" + studentAId)
                    .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
                .andExpect(jsonPath("$.message").value("JWT token signature is invalid or tampered"));
        }

        @Test
        @DisplayName("4. JWT signed with wrong secret returns 401 INVALID_TOKEN")
        void shouldReturn401OnWrongSecret() throws Exception {
            JwtProperties wrongProps = new JwtProperties(WRONG_SECRET, ISSUER, 3600L);
            JwtTokenProvider wrongProvider = new JwtTokenProvider(wrongProps, Clock.systemUTC());
            AuthenticationResult auth = new AuthenticationResult(userAId, "studentA", "a@univ.edu", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);
            String alienToken = wrongProvider.generateToken(auth);

            studentMockMvc.perform(get("/api/v1/students/" + studentAId)
                    .header("Authorization", "Bearer " + alienToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
                .andExpect(jsonPath("$.message").value("JWT token signature is invalid or tampered"));
        }

        @Test
        @DisplayName("5. Invalid issuer returns 401 INVALID_ISSUER")
        void shouldReturn401OnInvalidIssuer() throws Exception {
            JwtProperties badIssuerProps = new JwtProperties(TEST_SECRET, "imposter-auth-service", 3600L);
            JwtTokenProvider badIssuerProvider = new JwtTokenProvider(badIssuerProps, Clock.systemUTC());
            AuthenticationResult auth = new AuthenticationResult(userAId, "studentA", "a@univ.edu", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);
            String badIssuerToken = badIssuerProvider.generateToken(auth);

            studentMockMvc.perform(get("/api/v1/students/" + studentAId)
                    .header("Authorization", "Bearer " + badIssuerToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("INVALID_ISSUER"))
                .andExpect(jsonPath("$.message").value("Invalid token issuer"));
        }

        @Test
        @DisplayName("6. Token-version mismatch returns 401 TOKEN_EXPIRED")
        void shouldReturn401OnTokenVersionMismatch() throws Exception {
            String token = createToken(userAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);

            // DB has incremented version 2
            UserAccount account = new UserAccount(
                userAId, "studentA", "a@univ.edu", "hash", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(),
                UserAccountStatus.ACTIVE, 0, Optional.empty(), 2, Instant.now(), Instant.now()
            );
            when(userAccountRepositoryPort.findById(userAId)).thenReturn(Optional.of(account));

            studentMockMvc.perform(get("/api/v1/students/" + studentAId)
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"))
                .andExpect(jsonPath("$.message").value("Token has been revoked or invalidated by password change"));
        }

        @Test
        @DisplayName("7. Suspended account returns 401 ACCOUNT_SUSPENDED")
        void shouldReturn401OnSuspendedAccount() throws Exception {
            String token = createToken(userAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);

            UserAccount account = new UserAccount(
                userAId, "studentA", "a@univ.edu", "hash", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(),
                UserAccountStatus.SUSPENDED, 0, Optional.empty(), 1, Instant.now(), Instant.now()
            );
            when(userAccountRepositoryPort.findById(userAId)).thenReturn(Optional.of(account));

            studentMockMvc.perform(get("/api/v1/students/" + studentAId)
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("ACCOUNT_SUSPENDED"))
                .andExpect(jsonPath("$.message").value("User account is suspended"));
        }

        @Test
        @DisplayName("8. Deactivated account returns 401 ACCOUNT_DEACTIVATED")
        void shouldReturn401OnDeactivatedAccount() throws Exception {
            String token = createToken(userAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);

            UserAccount account = new UserAccount(
                userAId, "studentA", "a@univ.edu", "hash", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(),
                UserAccountStatus.DEACTIVATED, 0, Optional.empty(), 1, Instant.now(), Instant.now()
            );
            when(userAccountRepositoryPort.findById(userAId)).thenReturn(Optional.of(account));

            studentMockMvc.perform(get("/api/v1/students/" + studentAId)
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("ACCOUNT_DEACTIVATED"))
                .andExpect(jsonPath("$.message").value("User account is deactivated"));
        }

        @Test
        @DisplayName("9. Account not found returns 401 INVALID_TOKEN")
        void shouldReturn401WhenAccountDeleted() throws Exception {
            String token = createToken(userAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);
            when(userAccountRepositoryPort.findById(userAId)).thenReturn(Optional.empty());

            studentMockMvc.perform(get("/api/v1/students/" + studentAId)
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
                .andExpect(jsonPath("$.message").value("Account associated with token no longer exists"));
        }
    }

    @Nested
    @DisplayName("403 Forbidden / ACCESS_DENIED Scenarios")
    class AccessDeniedScenarios {

        @Test
        @DisplayName("10. Student IDOR on profile view: Student A attempting to view Student B returns 403 ACCESS_DENIED")
        void shouldDenyStudentAccessToOtherStudentProfile() throws Exception {
            String token = createToken(userAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);
            mockActiveAccount(userAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);

            studentMockMvc.perform(get("/api/v1/students/" + studentBId)
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Access denied: Students may only view their own profile"))
                .andExpect(jsonPath("$.path").value("/api/v1/students/" + studentBId))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()))
                .andExpect(jsonPath("$.details", empty()));
        }

        @Test
        @DisplayName("11. Student IDOR on profile patch: Student A attempting to update Student B returns 403 ACCESS_DENIED")
        void shouldDenyStudentUpdateToOtherStudentProfile() throws Exception {
            String token = createToken(userAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);
            mockActiveAccount(userAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);

            UpdateStudentRequest request = new UpdateStudentRequest("New Name", "new@univ.edu");

            studentMockMvc.perform(patch("/api/v1/students/" + studentBId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Access denied: Students may only update their own profile"));
        }

        @Test
        @DisplayName("12. Student IDOR on subject attendance: Student A accessing Student B returns 403 ACCESS_DENIED")
        void shouldDenyStudentAccessToOtherStudentSubjectAttendance() throws Exception {
            String token = createToken(userAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);
            mockActiveAccount(userAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);

            calculationMockMvc.perform(get("/api/v1/students/" + studentBId + "/attendance/subjects/" + subjectId)
                    .header("Authorization", "Bearer " + token)
                    .param("sectionId", sectionId.toString())
                    .param("policyId", policyId.toString())
                    .param("periodId", periodId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Access denied: You cannot view or access another student's attendance"));
        }

        @Test
        @DisplayName("13. Student IDOR on overall attendance: Student A accessing Student B returns 403 ACCESS_DENIED")
        void shouldDenyStudentAccessToOtherStudentOverallAttendance() throws Exception {
            String token = createToken(userAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);
            mockActiveAccount(userAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);

            calculationMockMvc.perform(get("/api/v1/students/" + studentBId + "/attendance/overall")
                    .header("Authorization", "Bearer " + token)
                    .param("sectionId", sectionId.toString())
                    .param("policyId", policyId.toString())
                    .param("periodId", periodId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Access denied: You cannot view or access another student's attendance"));
        }

        @Test
        @DisplayName("14. Student IDOR on shortage projection: Student A accessing Student B returns 403 ACCESS_DENIED")
        void shouldDenyStudentAccessToOtherStudentShortageProjection() throws Exception {
            String token = createToken(userAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);
            mockActiveAccount(userAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);

            calculationMockMvc.perform(get("/api/v1/students/" + studentBId + "/attendance/subjects/" + subjectId + "/shortage")
                    .header("Authorization", "Bearer " + token)
                    .param("sectionId", sectionId.toString())
                    .param("policyId", policyId.toString())
                    .param("periodId", periodId.toString())
                    .param("projectedRemainingUnits", "10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }

        @Test
        @DisplayName("15. Faculty impersonation: Faculty A creating session under Faculty B ID returns 403 ACCESS_DENIED")
        void shouldDenyFacultyImpersonatingAnotherFaculty() throws Exception {
            String token = createToken(userFacultyAId, "facultyA", UserRole.FACULTY, Optional.empty(), Optional.of(facultyAId), 1);
            mockActiveAccount(userFacultyAId, "facultyA", UserRole.FACULTY, Optional.empty(), Optional.of(facultyAId), 1);

            CreateSessionRequest request = new CreateSessionRequest(
                subjectId, sectionId, facultyBId, periodId, LocalDate.of(2026, 9, 15), "THEORY", 1, null
            );

            sessionMockMvc.perform(post("/api/v1/sessions")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Access denied: Faculty cannot create sessions on behalf of another faculty member"));
        }

        @Test
        @DisplayName("16. Unassigned faculty creating session returns 403 ACCESS_DENIED")
        void shouldDenyUnassignedFacultySessionCreation() throws Exception {
            String token = createToken(userFacultyAId, "facultyA", UserRole.FACULTY, Optional.empty(), Optional.of(facultyAId), 1);
            mockActiveAccount(userFacultyAId, "facultyA", UserRole.FACULTY, Optional.empty(), Optional.of(facultyAId), 1);

            when(facultyAssignmentPort.isFacultyAssigned(
                eq(facultyAId), eq(subjectId), eq(sectionId), eq(periodId), any(LocalDate.class)
            )).thenReturn(false);

            CreateSessionRequest request = new CreateSessionRequest(
                subjectId, sectionId, facultyAId, periodId, LocalDate.of(2026, 9, 15), "THEORY", 1, null
            );

            sessionMockMvc.perform(post("/api/v1/sessions")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Access denied: Faculty is not assigned to teach this subject and section"));
        }

        @Test
        @DisplayName("17. Student attempting to record attendance returns 403 ACCESS_DENIED")
        void shouldDenyStudentRecordingAttendance() throws Exception {
            String token = createToken(userAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);
            mockActiveAccount(userAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);

            Session session = new Session(sessionId, subjectId, sectionId, facultyAId, LocalDate.of(2026, 9, 1),
                SessionType.THEORY, 1, 0, SessionStatus.SCHEDULED, Optional.empty(), Optional.empty());
            when(sessionPort.findById(sessionId)).thenReturn(Optional.of(session));

            RecordAttendanceBatchRequest request = new RecordAttendanceBatchRequest(
                List.of(new com.amcs.application.dto.attendance.AttendanceRecordItemDto(studentAId, "PRESENT"))
            );

            attendanceMockMvc.perform(post("/api/v1/sessions/" + sessionId + "/attendance")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Access denied: Students are not permitted to record attendance"));
        }

        @Test
        @DisplayName("18. Student attempting to correct attendance returns 403 ACCESS_DENIED")
        void shouldDenyStudentCorrectingAttendance() throws Exception {
            String token = createToken(userAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);
            mockActiveAccount(userAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);

            AttendanceRecord record = new AttendanceRecord(recordId, sessionId, studentAId, AttendanceStatus.ABSENT);
            when(recordPort.findById(recordId)).thenReturn(Optional.of(record));

            Session session = new Session(sessionId, subjectId, sectionId, facultyAId, LocalDate.of(2026, 9, 1),
                SessionType.THEORY, 1, 1, SessionStatus.CONDUCTED, Optional.empty(), Optional.empty());
            when(sessionPort.findById(sessionId)).thenReturn(Optional.of(session));

            CorrectAttendanceRecordRequest request = new CorrectAttendanceRecordRequest("PRESENT", "Medical slip", adminId);

            attendanceMockMvc.perform(post("/api/v1/attendance/records/" + recordId + "/correction")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Access denied: Students are not permitted to correct attendance records"));
        }

        @Test
        @DisplayName("19. Unassigned/non-conducting faculty attempting to cancel session returns 403 ACCESS_DENIED")
        void shouldDenyUnauthorizedFacultyCancellingSession() throws Exception {
            String token = createToken(userFacultyBId, "facultyB", UserRole.FACULTY, Optional.empty(), Optional.of(facultyBId), 1);
            mockActiveAccount(userFacultyBId, "facultyB", UserRole.FACULTY, Optional.empty(), Optional.of(facultyBId), 1);

            Session session = new Session(sessionId, subjectId, sectionId, facultyAId, LocalDate.of(2026, 9, 1),
                SessionType.THEORY, 1, 0, SessionStatus.SCHEDULED, Optional.empty(), Optional.empty());
            when(sessionPort.findById(sessionId)).thenReturn(Optional.of(session));
            when(facultyAssignmentPort.findByFacultyId(facultyBId)).thenReturn(List.of());

            CancelSessionRequest request = new CancelSessionRequest("Illness");

            sessionMockMvc.perform(post("/api/v1/sessions/" + sessionId + "/cancel")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Access denied: Only assigned or conducting faculty may modify this session"));
        }
    }

    @Nested
    @DisplayName("Information Disclosure & Non-Leakage Verification")
    class InformationDisclosureTests {

        @Test
        @DisplayName("20. Security error responses never disclose tokens, passwords, secrets, or internal stack traces")
        void shouldNeverDiscloseSensitiveInformationInErrors() throws Exception {
            String token = createToken(userAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);
            mockActiveAccount(userAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);

            String responseContent = studentMockMvc.perform(get("/api/v1/students/" + studentBId)
                    .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();

            // Verify absence of sensitive tokens, secrets, or internal keywords
            org.assertj.core.api.Assertions.assertThat(responseContent)
                .doesNotContain(TEST_SECRET)
                .doesNotContain("hash")
                .doesNotContain("password")
                .doesNotContain("Exception")
                .doesNotContain("at com.amcs")
                .doesNotContain("Bearer ");
        }
    }
}
