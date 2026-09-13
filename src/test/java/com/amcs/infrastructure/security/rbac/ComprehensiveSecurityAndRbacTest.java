package com.amcs.infrastructure.security.rbac;

import com.amcs.application.dto.attendance.CorrectAttendanceRecordRequest;
import com.amcs.application.dto.attendance.RecordAttendanceBatchRequest;
import com.amcs.application.dto.enrollment.EnrollStudentRequest;
import com.amcs.application.dto.enrollment.TransferStudentRequest;
import com.amcs.application.dto.security.AuthenticationResult;
import com.amcs.application.dto.session.CancelSessionRequest;
import com.amcs.application.dto.session.CreateSessionRequest;
import com.amcs.application.dto.student.UpdateStudentRequest;
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
import com.amcs.application.port.out.security.FacultyAssignment;
import com.amcs.application.port.out.security.FacultyAssignmentRepositoryPort;
import com.amcs.application.port.out.security.UserAccount;
import com.amcs.application.port.out.security.UserAccountRepositoryPort;
import com.amcs.application.port.out.security.UserAccountStatus;
import com.amcs.application.port.out.security.UserRole;
import com.amcs.application.security.ApplicationAuthorizationService;
import com.amcs.application.service.AttendanceCalculationApplicationService;
import com.amcs.application.service.AttendanceRecordingApplicationService;
import com.amcs.application.service.EnrollmentApplicationService;
import com.amcs.application.service.SessionApplicationService;
import com.amcs.application.service.StudentApplicationService;
import com.amcs.domain.academic.AcademicPeriod;
import com.amcs.domain.academic.Subject;
import com.amcs.domain.attendance.AttendanceRecord;
import com.amcs.domain.attendance.AttendanceStatus;
import com.amcs.domain.attendance.Session;
import com.amcs.domain.attendance.SessionStatus;
import com.amcs.domain.attendance.SessionType;
import com.amcs.infrastructure.persistence.entity.StudentEntity;
import com.amcs.infrastructure.security.adapter.SpringSecurityCurrentUserAdapter;
import com.amcs.infrastructure.security.error.RestAuthenticationEntryPoint;
import com.amcs.infrastructure.security.jwt.JwtAuthenticationFilter;
import com.amcs.infrastructure.security.jwt.JwtProperties;
import com.amcs.infrastructure.security.jwt.JwtTokenProvider;
import com.amcs.infrastructure.web.controller.AttendanceCalculationController;
import com.amcs.infrastructure.web.controller.AttendanceController;
import com.amcs.infrastructure.web.controller.EnrollmentController;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ComprehensiveSecurityAndRbacTest {

    private static final String TEST_SECRET = "super-secret-key-that-is-at-least-256-bits-long-32-bytes!";
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
    private MockMvc enrollmentMockMvc;

    private JwtTokenProvider tokenProvider;
    private JwtProperties jwtProperties;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    // Student identities
    private final UUID studentAId = UUID.randomUUID();
    private final UUID userStudentAId = UUID.randomUUID();
    private final UUID studentBId = UUID.randomUUID();
    private final UUID userStudentBId = UUID.randomUUID();

    // Faculty identities
    private final UUID facultyAId = UUID.randomUUID(); // Assigned to Subject 1, Section 1
    private final UUID userFacultyAId = UUID.randomUUID();
    private final UUID facultyBId = UUID.randomUUID(); // Assigned to Subject 2, Section 2
    private final UUID userFacultyBId = UUID.randomUUID();
    private final UUID facultyCId = UUID.randomUUID(); // Unassigned
    private final UUID userFacultyCId = UUID.randomUUID();

    // Admin identity
    private final UUID adminId = UUID.randomUUID();
    private final UUID userAdminId = UUID.randomUUID();

    // Domain IDs
    private final UUID subject1Id = UUID.randomUUID();
    private final UUID subject2Id = UUID.randomUUID();
    private final UUID section1Id = UUID.randomUUID();
    private final UUID section2Id = UUID.randomUUID();
    private final UUID periodId = UUID.randomUUID();
    private final UUID policyId = UUID.randomUUID();
    private final UUID session1Id = UUID.randomUUID();
    private final UUID session2Id = UUID.randomUUID();
    private final UUID record1Id = UUID.randomUUID();

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
        EnrollmentApplicationService enrollmentService = new EnrollmentApplicationService(enrollmentPort, studentPort, sectionPort, authService);

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

        enrollmentMockMvc = MockMvcBuilders.standaloneSetup(new EnrollmentController(enrollmentService))
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

    private void mockUserAccount(UUID userId, String username, UserRole role, Optional<UUID> studentId, Optional<UUID> facultyId, int version) {
        UserAccount account = new UserAccount(
            userId, username, username + "@univ.edu", "hash", role, studentId, facultyId,
            UserAccountStatus.ACTIVE, 0, Optional.empty(), version, Instant.now(), Instant.now()
        );
        when(userAccountRepositoryPort.findById(userId)).thenReturn(Optional.of(account));
    }

    // =========================================================================
    // 1. STUDENT IDOR ATTACK SUITE
    // =========================================================================
    @Nested
    @DisplayName("Student IDOR Attack Suite")
    class StudentIdorAttackSuite {

        @Test
        @DisplayName("Student A cannot view Student B's enrollment history (403 ACCESS_DENIED)")
        void studentCannotViewOtherStudentEnrollments() throws Exception {
            String token = createToken(userStudentAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);
            mockUserAccount(userStudentAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);

            enrollmentMockMvc.perform(get("/api/v1/enrollments/students/" + studentBId)
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Access denied: Students may only view their own enrollment history"));
        }

        @Test
        @DisplayName("Student A cannot update Student B's profile through mass-assignment or path manipulation")
        void studentCannotUpdateOtherStudentProfile() throws Exception {
            String token = createToken(userStudentAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);
            mockUserAccount(userStudentAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);

            UpdateStudentRequest request = new UpdateStudentRequest("Hacked Name", "hacked@univ.edu");

            studentMockMvc.perform(patch("/api/v1/students/" + studentBId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

            verify(studentPort, never()).save(any());
        }

        @Test
        @DisplayName("Student A cannot access Student B's overview summary (403 ACCESS_DENIED)")
        void studentCannotAccessOtherStudentOverview() throws Exception {
            String token = createToken(userStudentAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);
            mockUserAccount(userStudentAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);

            calculationMockMvc.perform(get("/api/v1/students/" + studentBId + "/attendance/summary")
                    .header("Authorization", "Bearer " + token)
                    .param("sectionId", section1Id.toString())
                    .param("policyId", policyId.toString())
                    .param("periodId", periodId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }
    }

    // =========================================================================
    // 2. FACULTY SCOPE ATTACK SUITE
    // =========================================================================
    @Nested
    @DisplayName("Faculty Scope Attack Suite")
    class FacultyScopeAttackSuite {

        @Test
        @DisplayName("Faculty A cannot create session for Subject 2 / Section 2 (unassigned)")
        void facultyCannotCreateSessionForUnassignedSubjectSection() throws Exception {
            String token = createToken(userFacultyAId, "facultyA", UserRole.FACULTY, Optional.empty(), Optional.of(facultyAId), 1);
            mockUserAccount(userFacultyAId, "facultyA", UserRole.FACULTY, Optional.empty(), Optional.of(facultyAId), 1);

            // Faculty A is assigned to Subject 1 / Section 1, NOT Subject 2 / Section 2
            when(facultyAssignmentPort.isFacultyAssigned(
                eq(facultyAId), eq(subject2Id), eq(section2Id), eq(periodId), any(LocalDate.class)
            )).thenReturn(false);

            CreateSessionRequest request = new CreateSessionRequest(
                subject2Id, section2Id, facultyAId, periodId, LocalDate.of(2026, 9, 20), "THEORY", 1, null
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
        @DisplayName("Unassigned Faculty C cannot record attendance for Session 1")
        void unassignedFacultyCannotRecordAttendance() throws Exception {
            String token = createToken(userFacultyCId, "facultyC", UserRole.FACULTY, Optional.empty(), Optional.of(facultyCId), 1);
            mockUserAccount(userFacultyCId, "facultyC", UserRole.FACULTY, Optional.empty(), Optional.of(facultyCId), 1);

            Session session = new Session(session1Id, subject1Id, section1Id, facultyAId, LocalDate.of(2026, 9, 1),
                SessionType.THEORY, 1, 0, SessionStatus.SCHEDULED, Optional.empty(), Optional.empty());
            when(sessionPort.findById(session1Id)).thenReturn(Optional.of(session));
            when(facultyAssignmentPort.findByFacultyId(facultyCId)).thenReturn(List.of());

            RecordAttendanceBatchRequest request = new RecordAttendanceBatchRequest(
                List.of(new com.amcs.application.dto.attendance.AttendanceRecordItemDto(studentAId, "PRESENT"))
            );

            attendanceMockMvc.perform(post("/api/v1/sessions/" + session1Id + "/attendance")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Access denied: Faculty is not assigned or authorized to record attendance for this session"));
        }

        @Test
        @DisplayName("Faculty B cannot correct attendance record in Session 1 conducted by Faculty A")
        void facultyCannotCorrectOtherFacultySessionAttendance() throws Exception {
            String token = createToken(userFacultyBId, "facultyB", UserRole.FACULTY, Optional.empty(), Optional.of(facultyBId), 1);
            mockUserAccount(userFacultyBId, "facultyB", UserRole.FACULTY, Optional.empty(), Optional.of(facultyBId), 1);

            Session session = new Session(session1Id, subject1Id, section1Id, facultyAId, LocalDate.of(2026, 9, 1),
                SessionType.THEORY, 1, 1, SessionStatus.CONDUCTED, Optional.empty(), Optional.empty());
            AttendanceRecord record = new AttendanceRecord(record1Id, session1Id, studentAId, AttendanceStatus.ABSENT);

            when(recordPort.findById(record1Id)).thenReturn(Optional.of(record));
            when(sessionPort.findById(session1Id)).thenReturn(Optional.of(session));
            when(facultyAssignmentPort.findByFacultyId(facultyBId)).thenReturn(List.of());

            CorrectAttendanceRecordRequest request = new CorrectAttendanceRecordRequest("PRESENT", "Medical duty", adminId);

            attendanceMockMvc.perform(post("/api/v1/attendance/records/" + record1Id + "/correction")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Access denied: Faculty is not authorized to correct attendance for this session"));
        }
    }

    // =========================================================================
    // 3. FACULTY IMPERSONATION TESTS
    // =========================================================================
    @Nested
    @DisplayName("Faculty Impersonation Tests")
    class FacultyImpersonationTests {

        @Test
        @DisplayName("Faculty A cannot create a session claiming Faculty B as conductor")
        void facultyCannotImpersonateAnotherFacultyInSessionCreation() throws Exception {
            String token = createToken(userFacultyAId, "facultyA", UserRole.FACULTY, Optional.empty(), Optional.of(facultyAId), 1);
            mockUserAccount(userFacultyAId, "facultyA", UserRole.FACULTY, Optional.empty(), Optional.of(facultyAId), 1);

            CreateSessionRequest request = new CreateSessionRequest(
                subject1Id, section1Id, facultyBId, periodId, LocalDate.of(2026, 9, 20), "THEORY", 1, null
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
    }

    // =========================================================================
    // 4. CROSS-ROLE PRIVILEGE ESCALATION
    // =========================================================================
    @Nested
    @DisplayName("Cross-Role Privilege Escalation Tests")
    class PrivilegeEscalationTests {

        @Test
        @DisplayName("Student cannot enroll other students (admin endpoint)")
        void studentCannotCallEnrollmentEndpoint() throws Exception {
            String token = createToken(userStudentAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);
            mockUserAccount(userStudentAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);

            EnrollStudentRequest request = new EnrollStudentRequest(
                studentBId, section1Id, LocalDate.of(2026, 8, 1), null
            );

            enrollmentMockMvc.perform(post("/api/v1/enrollments")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Access denied: Only administrators may enroll students"));
        }

        @Test
        @DisplayName("Student cannot transfer other students (admin endpoint)")
        void studentCannotCallTransferEndpoint() throws Exception {
            String token = createToken(userStudentAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);
            mockUserAccount(userStudentAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);

            TransferStudentRequest request = new TransferStudentRequest(
                studentBId, section1Id, section2Id, LocalDate.of(2026, 9, 1)
            );

            enrollmentMockMvc.perform(post("/api/v1/enrollments/transfer")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Access denied: Only administrators may transfer students"));
        }

        @Test
        @DisplayName("Faculty cannot enroll students (admin endpoint)")
        void facultyCannotEnrollStudents() throws Exception {
            String token = createToken(userFacultyAId, "facultyA", UserRole.FACULTY, Optional.empty(), Optional.of(facultyAId), 1);
            mockUserAccount(userFacultyAId, "facultyA", UserRole.FACULTY, Optional.empty(), Optional.of(facultyAId), 1);

            EnrollStudentRequest request = new EnrollStudentRequest(
                studentAId, section1Id, LocalDate.of(2026, 8, 1), null
            );

            enrollmentMockMvc.perform(post("/api/v1/enrollments")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Access denied: Only administrators may enroll students"));
        }
    }

    // =========================================================================
    // 5. HOD_ADMIN PRIVILEGE & INVARIANT ENFORCEMENT
    // =========================================================================
    @Nested
    @DisplayName("HOD_ADMIN Privilege & Invariant Enforcement")
    class AdminPrivilegeAndInvariantsTests {

        @Test
        @DisplayName("Admin can view Student B's enrollment history")
        void adminCanViewAnyStudentEnrollmentHistory() throws Exception {
            String token = createToken(userAdminId, "admin", UserRole.HOD_ADMIN, Optional.empty(), Optional.empty(), 1);
            mockUserAccount(userAdminId, "admin", UserRole.HOD_ADMIN, Optional.empty(), Optional.empty(), 1);

            when(enrollmentPort.findByStudent(studentBId)).thenReturn(List.of());

            enrollmentMockMvc.perform(get("/api/v1/enrollments/students/" + studentBId)
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("Admin cannot bypass input validation (e.g. transfer to non-existent section yields 404, not bypass)")
        void adminDoesNotBypassResourceValidation() throws Exception {
            String token = createToken(userAdminId, "admin", UserRole.HOD_ADMIN, Optional.empty(), Optional.empty(), 1);
            mockUserAccount(userAdminId, "admin", UserRole.HOD_ADMIN, Optional.empty(), Optional.empty(), 1);

            when(studentPort.findById(studentAId)).thenReturn(Optional.of(
                new StudentEntity(studentAId, "REG123", "Alice", "alice@univ.edu", UUID.randomUUID())
            ));
            when(sectionPort.findById(section2Id)).thenReturn(Optional.empty());

            TransferStudentRequest request = new TransferStudentRequest(
                studentAId, section1Id, section2Id, LocalDate.of(2026, 9, 1)
            );

            enrollmentMockMvc.perform(post("/api/v1/enrollments/transfer")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }
    }

    // =========================================================================
    // 6. MASS ASSIGNMENT / OVERPOSTING TESTS
    // =========================================================================
    @Nested
    @DisplayName("Mass Assignment / Overposting Tests")
    class MassAssignmentTests {

        @Test
        @DisplayName("Overposting 'role' or 'status' in JSON payload does not elevate student privilege")
        void overpostingRoleDoesNotElevatePrivilege() throws Exception {
            String token = createToken(userStudentAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);
            mockUserAccount(userStudentAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);

            when(studentPort.findById(studentAId)).thenReturn(Optional.of(
                new StudentEntity(studentAId, "REG123", "Alice", "alice@univ.edu", UUID.randomUUID())
            ));
            when(studentPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Malicious JSON containing injected role, status, tokenVersion
            String maliciousJson = """
                {
                    "name": "Alice Updated",
                    "email": "alice.updated@univ.edu",
                    "role": "HOD_ADMIN",
                    "status": "ACTIVE",
                    "tokenVersion": 999
                }
                """;

            studentMockMvc.perform(patch("/api/v1/students/" + studentAId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(maliciousJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Updated"));

            // Verify UserAccount was never modified or touched
            verify(userAccountRepositoryPort, never()).save(any());
        }
    }

    // =========================================================================
    // 7. RESOURCE OWNERSHIP MATRIX TESTS
    // =========================================================================
    @Nested
    @DisplayName("Resource Ownership Matrix Tests")
    class ResourceOwnershipMatrixTests {

        @Test
        @DisplayName("Matrix: Student A -> Student A profile (ALLOW)")
        void matrixStudentToOwnAttendance() throws Exception {
            String token = createToken(userStudentAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);
            mockUserAccount(userStudentAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);

            when(studentPort.findById(studentAId)).thenReturn(Optional.of(
                new StudentEntity(studentAId, "REG1", "Alice", "alice@univ.edu", UUID.randomUUID())
            ));

            studentMockMvc.perform(get("/api/v1/students/" + studentAId)
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(studentAId.toString()));
        }

        @Test
        @DisplayName("Matrix: Student A -> Student B profile (DENY 403)")
        void matrixStudentToOtherAttendance() throws Exception {
            String token = createToken(userStudentAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);
            mockUserAccount(userStudentAId, "studentA", UserRole.STUDENT, Optional.of(studentAId), Optional.empty(), 1);

            studentMockMvc.perform(get("/api/v1/students/" + studentBId)
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }

        @Test
        @DisplayName("Matrix: Faculty A -> assigned Subject 1 session modify (ALLOW)")
        void matrixFacultyToAssignedSession() throws Exception {
            String token = createToken(userFacultyAId, "facultyA", UserRole.FACULTY, Optional.empty(), Optional.of(facultyAId), 1);
            mockUserAccount(userFacultyAId, "facultyA", UserRole.FACULTY, Optional.empty(), Optional.of(facultyAId), 1);

            Session session = new Session(session1Id, subject1Id, section1Id, facultyAId, LocalDate.of(2026, 9, 1),
                SessionType.THEORY, 1, 0, SessionStatus.SCHEDULED, Optional.empty(), Optional.empty());
            when(sessionPort.findById(session1Id)).thenReturn(Optional.of(session));
            when(sessionPort.save(any(), any())).thenAnswer(inv -> inv.getArgument(0));

            CancelSessionRequest request = new CancelSessionRequest("Illness");

            sessionMockMvc.perform(post("/api/v1/sessions/" + session1Id + "/cancel")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Matrix: Faculty A -> unassigned Subject 2 session modify (DENY 403)")
        void matrixFacultyToUnassignedSession() throws Exception {
            String token = createToken(userFacultyAId, "facultyA", UserRole.FACULTY, Optional.empty(), Optional.of(facultyAId), 1);
            mockUserAccount(userFacultyAId, "facultyA", UserRole.FACULTY, Optional.empty(), Optional.of(facultyAId), 1);

            // Session 2 is for Subject 2 / Section 2 conducted by Faculty B
            Session session = new Session(session2Id, subject2Id, section2Id, facultyBId, LocalDate.of(2026, 9, 1),
                SessionType.THEORY, 1, 0, SessionStatus.SCHEDULED, Optional.empty(), Optional.empty());
            when(sessionPort.findById(session2Id)).thenReturn(Optional.of(session));
            when(facultyAssignmentPort.findByFacultyId(facultyAId)).thenReturn(List.of());

            CancelSessionRequest request = new CancelSessionRequest("Illness");

            sessionMockMvc.perform(post("/api/v1/sessions/" + session2Id + "/cancel")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }
    }
}
