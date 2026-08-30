package com.amcs.infrastructure.security.adapter;

import com.amcs.application.dto.security.AuthenticationResult;
import com.amcs.application.port.out.security.AuthenticatedActor;
import com.amcs.application.port.out.security.CurrentUserPort;
import com.amcs.application.port.out.security.UserAccount;
import com.amcs.application.port.out.security.UserAccountRepositoryPort;
import com.amcs.application.port.out.security.UserAccountStatus;
import com.amcs.application.port.out.security.UserRole;
import com.amcs.infrastructure.security.error.RestAuthenticationEntryPoint;
import com.amcs.infrastructure.security.jwt.JwtAuthenticationFilter;
import com.amcs.infrastructure.security.jwt.JwtProperties;
import com.amcs.infrastructure.security.jwt.JwtTokenProvider;
import com.amcs.infrastructure.web.error.GlobalRestExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CurrentUserIntegrationTest {

    private static final String TEST_SECRET = "super-secret-key-that-is-at-least-256-bits-long-32-bytes!";
    private static final String ISSUER = "amcs-auth-service";

    @Mock
    private UserAccountRepositoryPort userAccountRepositoryPort;

    private MockMvc mockMvc;
    private JwtTokenProvider tokenProvider;
    private CurrentUserPort currentUserPort;

    private final UUID userId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();

    /**
     * Test-only controller demonstrating application use case consuming CurrentUserPort.
     */
    @RestController
    @RequestMapping("/test/security")
    static class TestActorInspectionController {
        private final CurrentUserPort currentUserPort;

        TestActorInspectionController(CurrentUserPort currentUserPort) {
            this.currentUserPort = currentUserPort;
        }

        @GetMapping("/whoami")
        public ResponseEntity<Map<String, Object>> whoAmI() {
            AuthenticatedActor actor = currentUserPort.requireCurrentActor();
            return ResponseEntity.ok(Map.of(
                "userId", actor.userId().toString(),
                "username", actor.username(),
                "role", actor.role().name(),
                "studentId", actor.studentId().map(UUID::toString).orElse(""),
                "isStudent", actor.isStudent()
            ));
        }
    }

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        JwtProperties jwtProperties = new JwtProperties(TEST_SECRET, ISSUER, 3600L);
        tokenProvider = new JwtTokenProvider(jwtProperties, Clock.systemUTC());

        RestAuthenticationEntryPoint authenticationEntryPoint = new RestAuthenticationEntryPoint(objectMapper);
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(
            tokenProvider,
            userAccountRepositoryPort,
            authenticationEntryPoint
        );

        currentUserPort = new SpringSecurityCurrentUserAdapter();
        TestActorInspectionController testController = new TestActorInspectionController(currentUserPort);

        mockMvc = MockMvcBuilders.standaloneSetup(testController)
            .setControllerAdvice(new GlobalRestExceptionHandler())
            .addFilter(jwtFilter)
            .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("End-to-end flow: HTTP Bearer JWT -> JwtFilter -> SecurityContext -> CurrentUserPort -> AuthenticatedActor")
    void shouldResolveAuthenticatedActorThroughPortFromHttpRequest() throws Exception {
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

        mockMvc.perform(get("/test/security/whoami")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(userId.toString()))
            .andExpect(jsonPath("$.username").value("CS2026-001"))
            .andExpect(jsonPath("$.role").value("STUDENT"))
            .andExpect(jsonPath("$.studentId").value(studentId.toString()))
            .andExpect(jsonPath("$.isStudent").value(true));
    }
}
