package com.amcs.infrastructure.security.rbac;

import com.amcs.application.dto.academic.CreateAcademicPeriodRequest;
import com.amcs.application.dto.academic.CreateDepartmentRequest;
import com.amcs.application.dto.academic.CreateSectionRequest;
import com.amcs.application.dto.academic.CreateSubjectRequest;
import com.amcs.application.dto.faculty.CreateFacultyRequest;
import com.amcs.application.dto.labgroup.AssignLabGroupRequest;
import com.amcs.application.dto.labgroup.CreateLabGroupRequest;
import com.amcs.application.dto.policy.CreateAttendancePolicyRequest;
import com.amcs.application.dto.policy.CreateOverallAttendancePolicyRequest;
import com.amcs.application.dto.security.AuthenticationResult;
import com.amcs.application.port.out.AcademicPeriodRepositoryPort;
import com.amcs.application.port.out.AttendancePolicyRepositoryPort;
import com.amcs.application.port.out.DepartmentRepositoryPort;
import com.amcs.application.port.out.FacultyRepositoryPort;
import com.amcs.application.port.out.LabGroupRepositoryPort;
import com.amcs.application.port.out.OverallAttendancePolicyRepositoryPort;
import com.amcs.application.port.out.SectionRepositoryPort;
import com.amcs.application.port.out.StudentRepositoryPort;
import com.amcs.application.port.out.SubjectRepositoryPort;
import com.amcs.application.port.out.security.CurrentUserPort;
import com.amcs.application.port.out.security.FacultyAssignmentRepositoryPort;
import com.amcs.application.port.out.security.UserAccount;
import com.amcs.application.port.out.security.UserAccountRepositoryPort;
import com.amcs.application.port.out.security.UserAccountStatus;
import com.amcs.application.port.out.security.UserRole;
import com.amcs.application.security.ApplicationAuthorizationService;
import com.amcs.application.service.AcademicStructureApplicationService;
import com.amcs.application.service.FacultyApplicationService;
import com.amcs.application.service.LabGroupApplicationService;
import com.amcs.application.service.PolicyApplicationService;
import com.amcs.domain.academic.AcademicPeriod;
import com.amcs.domain.academic.CourseType;
import com.amcs.domain.academic.Subject;
import com.amcs.infrastructure.persistence.entity.DepartmentEntity;
import com.amcs.infrastructure.persistence.entity.FacultyEntity;
import com.amcs.infrastructure.persistence.entity.LabGroupEntity;
import com.amcs.infrastructure.persistence.entity.LabGroupMembershipEntity;
import com.amcs.infrastructure.persistence.entity.OverallAttendancePolicyEntity;
import com.amcs.infrastructure.persistence.entity.SectionEntity;
import com.amcs.infrastructure.persistence.entity.StudentEntity;
import com.amcs.infrastructure.security.adapter.SpringSecurityCurrentUserAdapter;
import com.amcs.infrastructure.security.error.RestAuthenticationEntryPoint;
import com.amcs.infrastructure.security.jwt.JwtAuthenticationFilter;
import com.amcs.infrastructure.security.jwt.JwtProperties;
import com.amcs.infrastructure.security.jwt.JwtTokenProvider;
import com.amcs.infrastructure.web.controller.AcademicStructureController;
import com.amcs.infrastructure.web.controller.FacultyController;
import com.amcs.infrastructure.web.controller.LabGroupController;
import com.amcs.infrastructure.web.controller.PolicyController;
import com.amcs.infrastructure.web.error.GlobalRestExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdministrativeRbacIntegrationTest {

    @Mock private UserAccountRepositoryPort userAccountPort;
    @Mock private FacultyAssignmentRepositoryPort facultyAssignmentPort;
    @Mock private DepartmentRepositoryPort departmentPort;
    @Mock private AcademicPeriodRepositoryPort periodPort;
    @Mock private SectionRepositoryPort sectionPort;
    @Mock private SubjectRepositoryPort subjectPort;
    @Mock private FacultyRepositoryPort facultyPort;
    @Mock private LabGroupRepositoryPort labGroupPort;
    @Mock private StudentRepositoryPort studentPort;
    @Mock private AttendancePolicyRepositoryPort policyPort;
    @Mock private OverallAttendancePolicyRepositoryPort overallPolicyPort;

    private JwtTokenProvider jwtTokenProvider;
    private MockMvc academicMockMvc;
    private MockMvc facultyMockMvc;
    private MockMvc labGroupMockMvc;
    private MockMvc policyMockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final String rawSecret = "super-secret-test-key-must-be-at-least-256-bits-long-32-bytes!";
    private final UUID userStudentId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID userFacultyId = UUID.randomUUID();
    private final UUID facultyId = UUID.randomUUID();
    private final UUID userAdminId = UUID.randomUUID();

    private final UUID deptId = UUID.randomUUID();
    private final UUID periodId = UUID.randomUUID();
    private final UUID sectionId = UUID.randomUUID();
    private final UUID labGroupId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties(rawSecret, "amcs-auth-service", 3600);
        jwtTokenProvider = new JwtTokenProvider(jwtProperties, Clock.systemUTC());
        CurrentUserPort currentUserPort = new SpringSecurityCurrentUserAdapter();

        ApplicationAuthorizationService authorizationService = new ApplicationAuthorizationService(
            currentUserPort,
            facultyAssignmentPort
        );

        AcademicStructureApplicationService academicService = new AcademicStructureApplicationService(
            departmentPort, periodPort, sectionPort, subjectPort, authorizationService
        );
        FacultyApplicationService facultyService = new FacultyApplicationService(
            facultyPort, departmentPort, authorizationService
        );
        LabGroupApplicationService labGroupService = new LabGroupApplicationService(
            labGroupPort, sectionPort, studentPort, authorizationService
        );
        PolicyApplicationService policyService = new PolicyApplicationService(
            policyPort, overallPolicyPort, authorizationService
        );

        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(
            jwtTokenProvider,
            userAccountPort,
            new RestAuthenticationEntryPoint(objectMapper)
        );

        academicMockMvc = MockMvcBuilders.standaloneSetup(new AcademicStructureController(academicService))
            .setControllerAdvice(new GlobalRestExceptionHandler())
            .addFilters(jwtFilter)
            .build();

        facultyMockMvc = MockMvcBuilders.standaloneSetup(new FacultyController(facultyService))
            .setControllerAdvice(new GlobalRestExceptionHandler())
            .addFilters(jwtFilter)
            .build();

        labGroupMockMvc = MockMvcBuilders.standaloneSetup(new LabGroupController(labGroupService))
            .setControllerAdvice(new GlobalRestExceptionHandler())
            .addFilters(jwtFilter)
            .build();

        policyMockMvc = MockMvcBuilders.standaloneSetup(new PolicyController(policyService))
            .setControllerAdvice(new GlobalRestExceptionHandler())
            .addFilters(jwtFilter)
            .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private String createToken(UUID userId, String username, UserRole role, Optional<UUID> sId, Optional<UUID> fId) {
        return jwtTokenProvider.generateToken(new AuthenticationResult(
            userId, username, username + "@univ.edu", role, sId, fId, 1
        ));
    }

    private void mockUser(UUID userId, String username, UserRole role, Optional<UUID> sId, Optional<UUID> fId) {
        when(userAccountPort.findById(userId)).thenReturn(Optional.of(new UserAccount(
            userId, username, username + "@univ.edu", "hashed", role, sId, fId,
            UserAccountStatus.ACTIVE, 0, Optional.empty(), 1, Instant.now(), Instant.now()
        )));
    }

    // =========================================================================
    // 1. ACADEMIC STRUCTURE RBAC TESTS
    // =========================================================================
    @Nested
    @DisplayName("Academic Structure RBAC")
    class AcademicStructureTests {

        @Test
        @DisplayName("POST /departments: Student is denied 403 ACCESS_DENIED")
        void createDepartmentStudentDenied() throws Exception {
            String token = createToken(userStudentId, "student", UserRole.STUDENT, Optional.of(studentId), Optional.empty());
            mockUser(userStudentId, "student", UserRole.STUDENT, Optional.of(studentId), Optional.empty());

            CreateDepartmentRequest request = new CreateDepartmentRequest("CSE", "Computer Science");

            academicMockMvc.perform(post("/api/v1/academic/departments")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Access denied: Only administrators may create departments"));
        }

        @Test
        @DisplayName("POST /departments: Faculty is denied 403 ACCESS_DENIED")
        void createDepartmentFacultyDenied() throws Exception {
            String token = createToken(userFacultyId, "faculty", UserRole.FACULTY, Optional.empty(), Optional.of(facultyId));
            mockUser(userFacultyId, "faculty", UserRole.FACULTY, Optional.empty(), Optional.of(facultyId));

            CreateDepartmentRequest request = new CreateDepartmentRequest("CSE", "Computer Science");

            academicMockMvc.perform(post("/api/v1/academic/departments")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }

        @Test
        @DisplayName("POST /departments: HOD_ADMIN is allowed 201 Created")
        void createDepartmentAdminAllowed() throws Exception {
            String token = createToken(userAdminId, "admin", UserRole.HOD_ADMIN, Optional.empty(), Optional.empty());
            mockUser(userAdminId, "admin", UserRole.HOD_ADMIN, Optional.empty(), Optional.empty());

            CreateDepartmentRequest request = new CreateDepartmentRequest("CSE", "Computer Science");
            DepartmentEntity saved = new DepartmentEntity(deptId, "CSE", "Computer Science");
            when(departmentPort.findByCode("CSE")).thenReturn(Optional.empty());
            when(departmentPort.save(any())).thenReturn(saved);

            academicMockMvc.perform(post("/api/v1/academic/departments")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(deptId.toString()))
                .andExpect(jsonPath("$.code").value("CSE"));
        }

        @Test
        @DisplayName("POST /periods: Student is denied 403 ACCESS_DENIED")
        void createPeriodStudentDenied() throws Exception {
            String token = createToken(userStudentId, "student", UserRole.STUDENT, Optional.of(studentId), Optional.empty());
            mockUser(userStudentId, "student", UserRole.STUDENT, Optional.of(studentId), Optional.empty());

            CreateAcademicPeriodRequest request = new CreateAcademicPeriodRequest(
                "Fall 2026", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 15)
            );

            academicMockMvc.perform(post("/api/v1/academic/periods")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }

        @Test
        @DisplayName("POST /sections: Student is denied 403 ACCESS_DENIED")
        void createSectionStudentDenied() throws Exception {
            String token = createToken(userStudentId, "student", UserRole.STUDENT, Optional.of(studentId), Optional.empty());
            mockUser(userStudentId, "student", UserRole.STUDENT, Optional.of(studentId), Optional.empty());

            CreateSectionRequest request = new CreateSectionRequest("Section A", deptId, periodId);

            academicMockMvc.perform(post("/api/v1/academic/sections")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }

        @Test
        @DisplayName("POST /subjects: Student is denied 403 ACCESS_DENIED")
        void createSubjectStudentDenied() throws Exception {
            String token = createToken(userStudentId, "student", UserRole.STUDENT, Optional.of(studentId), Optional.empty());
            mockUser(userStudentId, "student", UserRole.STUDENT, Optional.of(studentId), Optional.empty());

            CreateSubjectRequest request = new CreateSubjectRequest("CS101", "Intro to CS", "THEORY", 3, deptId);

            academicMockMvc.perform(post("/api/v1/academic/subjects")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }
    }

    // =========================================================================
    // 2. FACULTY ONBOARDING RBAC TESTS
    // =========================================================================
    @Nested
    @DisplayName("Faculty Onboarding RBAC")
    class FacultyRbacTests {

        @Test
        @DisplayName("POST /faculty: Student is denied 403 ACCESS_DENIED")
        void createFacultyStudentDenied() throws Exception {
            String token = createToken(userStudentId, "student", UserRole.STUDENT, Optional.of(studentId), Optional.empty());
            mockUser(userStudentId, "student", UserRole.STUDENT, Optional.of(studentId), Optional.empty());

            CreateFacultyRequest request = new CreateFacultyRequest("FAC001", "Dr. Alan", "alan@univ.edu", deptId);

            facultyMockMvc.perform(post("/api/v1/faculty")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Access denied: Only administrators may create faculty members"));
        }

        @Test
        @DisplayName("POST /faculty: Faculty is denied 403 ACCESS_DENIED")
        void createFacultyFacultyDenied() throws Exception {
            String token = createToken(userFacultyId, "faculty", UserRole.FACULTY, Optional.empty(), Optional.of(facultyId));
            mockUser(userFacultyId, "faculty", UserRole.FACULTY, Optional.empty(), Optional.of(facultyId));

            CreateFacultyRequest request = new CreateFacultyRequest("FAC001", "Dr. Alan", "alan@univ.edu", deptId);

            facultyMockMvc.perform(post("/api/v1/faculty")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }

        @Test
        @DisplayName("POST /faculty: HOD_ADMIN is allowed 201 Created")
        void createFacultyAdminAllowed() throws Exception {
            String token = createToken(userAdminId, "admin", UserRole.HOD_ADMIN, Optional.empty(), Optional.empty());
            mockUser(userAdminId, "admin", UserRole.HOD_ADMIN, Optional.empty(), Optional.empty());

            CreateFacultyRequest request = new CreateFacultyRequest("FAC001", "Dr. Alan", "alan@univ.edu", deptId);
            FacultyEntity saved = new FacultyEntity(facultyId, "FAC001", "Dr. Alan", "alan@univ.edu", deptId);
            when(departmentPort.findById(deptId)).thenReturn(Optional.of(new DepartmentEntity(deptId, "CSE", "Computer Science")));
            when(facultyPort.findByEmployeeId("FAC001")).thenReturn(Optional.empty());
            when(facultyPort.save(any())).thenReturn(saved);

            facultyMockMvc.perform(post("/api/v1/faculty")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(facultyId.toString()))
                .andExpect(jsonPath("$.employeeId").value("FAC001"));
        }
    }

    // =========================================================================
    // 3. LAB GROUP RBAC TESTS
    // =========================================================================
    @Nested
    @DisplayName("Lab Group RBAC")
    class LabGroupRbacTests {

        @Test
        @DisplayName("POST /lab-groups: Student is denied 403 ACCESS_DENIED")
        void createLabGroupStudentDenied() throws Exception {
            String token = createToken(userStudentId, "student", UserRole.STUDENT, Optional.of(studentId), Optional.empty());
            mockUser(userStudentId, "student", UserRole.STUDENT, Optional.of(studentId), Optional.empty());

            CreateLabGroupRequest request = new CreateLabGroupRequest("Group A", sectionId);

            labGroupMockMvc.perform(post("/api/v1/lab-groups")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Access denied: Only administrators may create lab groups"));
        }

        @Test
        @DisplayName("POST /lab-groups/assign: Faculty is denied 403 ACCESS_DENIED")
        void assignLabGroupFacultyDenied() throws Exception {
            String token = createToken(userFacultyId, "faculty", UserRole.FACULTY, Optional.empty(), Optional.of(facultyId));
            mockUser(userFacultyId, "faculty", UserRole.FACULTY, Optional.empty(), Optional.of(facultyId));

            AssignLabGroupRequest request = new AssignLabGroupRequest(studentId, labGroupId, LocalDate.of(2026, 8, 1), null);

            labGroupMockMvc.perform(post("/api/v1/lab-groups/assign")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Access denied: Only administrators may assign students to lab groups"));
        }

        @Test
        @DisplayName("POST /lab-groups: HOD_ADMIN is allowed 201 Created")
        void createLabGroupAdminAllowed() throws Exception {
            String token = createToken(userAdminId, "admin", UserRole.HOD_ADMIN, Optional.empty(), Optional.empty());
            mockUser(userAdminId, "admin", UserRole.HOD_ADMIN, Optional.empty(), Optional.empty());

            CreateLabGroupRequest request = new CreateLabGroupRequest("Group A", sectionId);
            LabGroupEntity saved = new LabGroupEntity(labGroupId, "Group A", sectionId);
            when(sectionPort.findById(sectionId)).thenReturn(Optional.of(new SectionEntity(sectionId, "Sec A", deptId, periodId)));
            when(labGroupPort.saveGroup(any())).thenReturn(saved);

            labGroupMockMvc.perform(post("/api/v1/lab-groups")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(labGroupId.toString()))
                .andExpect(jsonPath("$.name").value("Group A"));
        }
    }

    // =========================================================================
    // 4. POLICY RBAC TESTS
    // =========================================================================
    @Nested
    @DisplayName("Policy RBAC")
    class PolicyRbacTests {

        @Test
        @DisplayName("POST /policies: Student is denied 403 ACCESS_DENIED")
        void createPolicyStudentDenied() throws Exception {
            String token = createToken(userStudentId, "student", UserRole.STUDENT, Optional.of(studentId), Optional.empty());
            mockUser(userStudentId, "student", UserRole.STUDENT, Optional.of(studentId), Optional.empty());

            CreateAttendancePolicyRequest request = new CreateAttendancePolicyRequest(
                "Standard Policy",
                new BigDecimal("75.00"),
                Map.of("PRESENT", new BigDecimal("1.00"), "ABSENT", new BigDecimal("0.00")),
                "TREAT_AS_ABSENT",
                Instant.now(),
                null
            );

            policyMockMvc.perform(post("/api/v1/policies")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Access denied: Only administrators may create attendance policies"));
        }

        @Test
        @DisplayName("POST /policies/overall: Faculty is denied 403 ACCESS_DENIED")
        void createOverallPolicyFacultyDenied() throws Exception {
            String token = createToken(userFacultyId, "faculty", UserRole.FACULTY, Optional.empty(), Optional.of(facultyId));
            mockUser(userFacultyId, "faculty", UserRole.FACULTY, Optional.empty(), Optional.of(facultyId));

            CreateOverallAttendancePolicyRequest request = new CreateOverallAttendancePolicyRequest(
                "Overall Policy",
                "ARITHMETIC_MEAN",
                new BigDecimal("75.00"),
                Instant.now(),
                null
            );

            policyMockMvc.perform(post("/api/v1/policies/overall")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Access denied: Only administrators may create overall attendance policies"));
        }

        @Test
        @DisplayName("POST /policies/overall: HOD_ADMIN is allowed 201 Created")
        void createOverallPolicyAdminAllowed() throws Exception {
            String token = createToken(userAdminId, "admin", UserRole.HOD_ADMIN, Optional.empty(), Optional.empty());
            mockUser(userAdminId, "admin", UserRole.HOD_ADMIN, Optional.empty(), Optional.empty());

            CreateOverallAttendancePolicyRequest request = new CreateOverallAttendancePolicyRequest(
                "Overall Policy",
                "ARITHMETIC_MEAN",
                new BigDecimal("75.00"),
                Instant.now(),
                null
            );
            UUID overallId = UUID.randomUUID();
            OverallAttendancePolicyEntity entity = new OverallAttendancePolicyEntity(
                overallId, "Overall Policy", 1, "ARITHMETIC_MEAN", new BigDecimal("75.00"),
                Instant.now(), null, true
            );
            when(overallPolicyPort.findAllVersions("Overall Policy")).thenReturn(List.of());
            when(overallPolicyPort.save(any())).thenReturn(entity);

            policyMockMvc.perform(post("/api/v1/policies/overall")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(overallId.toString()))
                .andExpect(jsonPath("$.name").value("Overall Policy"));
        }
    }
}
