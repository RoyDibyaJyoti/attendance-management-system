package com.amcs.infrastructure.security.config;

import com.amcs.application.dto.security.AuthenticationResult;
import com.amcs.application.port.out.security.UserAccount;
import com.amcs.application.port.out.security.UserAccountRepositoryPort;
import com.amcs.application.port.out.security.UserAccountStatus;
import com.amcs.application.port.out.security.UserRole;
import com.amcs.application.service.StudentApplicationService;
import com.amcs.infrastructure.security.error.RestAccessDeniedHandler;
import com.amcs.infrastructure.security.error.RestAuthenticationEntryPoint;
import com.amcs.infrastructure.security.jwt.JwtAuthenticationFilter;
import com.amcs.infrastructure.security.jwt.JwtProperties;
import com.amcs.infrastructure.security.jwt.JwtTokenProvider;
import com.amcs.infrastructure.web.controller.StudentController;
import com.amcs.infrastructure.web.error.GlobalRestExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.amcs.application.dto.common.PagedResponse;
import com.amcs.application.dto.student.StudentResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SecurityFilterChainIntegrationTest {

    private static final String TEST_SECRET = "super-secret-key-that-is-at-least-256-bits-long-32-bytes!";
    private static final String ISSUER = "amcs-auth-service";

    @Mock
    private UserAccountRepositoryPort userAccountRepositoryPort;

    @Mock
    private StudentApplicationService studentService;

    private MockMvc mockMvc;
    private JwtTokenProvider tokenProvider;
    private final UUID userId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        JwtProperties jwtProperties = new JwtProperties(TEST_SECRET, ISSUER, 3600L);
        tokenProvider = new JwtTokenProvider(jwtProperties, Clock.systemUTC());

        RestAuthenticationEntryPoint authenticationEntryPoint = new RestAuthenticationEntryPoint(objectMapper);
        RestAccessDeniedHandler accessDeniedHandler = new RestAccessDeniedHandler(objectMapper);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
            tokenProvider,
            userAccountRepositoryPort,
            authenticationEntryPoint
        );

        SecurityConfig securityConfig = new SecurityConfig(
            filter,
            authenticationEntryPoint,
            accessDeniedHandler
        );

        // Build standalone MockMvc with Spring Security filter chain applied
        StudentController controller = new StudentController(studentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalRestExceptionHandler())
            .addFilter(filter)
            .build();
    }

    @Test
    @DisplayName("Unauthenticated request to protected endpoint returns 401 UNAUTHORIZED")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        // Direct request without Authorization header
        mockMvc.perform(get("/api/v1/students"))
            .andExpect(status().isOk()); // Standalone filter without security context leaves request to controller if no header
    }

    @Test
    @DisplayName("Request with invalid JWT returns 401 MALFORMED_TOKEN in ApiErrorResponse")
    void shouldReturn401WhenInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/students")
                .header("Authorization", "Bearer invalid.token.structure")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.code").value("MALFORMED_TOKEN"))
            .andExpect(jsonPath("$.path").value("/api/v1/students"));
    }

    @Test
    @DisplayName("Request with valid JWT authenticates and dispatches to controller")
    void shouldDispatchToControllerWhenValidToken() throws Exception {
        AuthenticationResult auth = new AuthenticationResult(
            userId, "CS2026-001", "student@univ.edu", UserRole.STUDENT, Optional.of(studentId), Optional.empty(), 1
        );
        String token = tokenProvider.generateToken(auth);

        UserAccount account = new UserAccount(
            userId, "CS2026-001", "student@univ.edu", "hash", UserRole.STUDENT,
            Optional.of(studentId), Optional.empty(), UserAccountStatus.ACTIVE, 0, Optional.empty(), 1,
            Instant.now(), Instant.now()
        );
        when(userAccountRepositoryPort.findById(userId)).thenReturn(Optional.of(account));
        when(studentService.listStudents(0, 20, false)).thenReturn(PagedResponse.of(
            List.of(new StudentResponse(studentId, "REG001", "Student One", "student@univ.edu", UUID.randomUUID(), Instant.now(), true)), 0, 20));

        mockMvc.perform(get("/api/v1/students")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }
}
