package com.amcs.infrastructure.web.controller;

import com.amcs.application.dto.security.ChangePasswordRequest;
import com.amcs.application.dto.security.LoginRequest;
import com.amcs.application.port.out.security.CurrentUserPort;
import com.amcs.application.port.out.security.UserAccount;
import com.amcs.application.port.out.security.UserAccountRepositoryPort;
import com.amcs.application.port.out.security.UserAccountStatus;
import com.amcs.application.port.out.security.UserRole;
import com.amcs.application.service.AuthenticationApplicationService;
import com.amcs.application.service.PasswordValidator;
import com.amcs.infrastructure.security.adapter.SpringSecurityCurrentUserAdapter;
import com.amcs.infrastructure.security.error.RestAuthenticationEntryPoint;
import com.amcs.infrastructure.security.jwt.JwtAuthenticationFilter;
import com.amcs.infrastructure.security.jwt.JwtProperties;
import com.amcs.infrastructure.security.jwt.JwtTokenProvider;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerIntegrationTest {

    @Mock
    private UserAccountRepositoryPort userAccountRepositoryPort;

    private AuthenticationApplicationService authService;
    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;
    private CurrentUserPort currentUserPort;
    private PasswordEncoder passwordEncoder;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String rawSecret = "super-secret-test-key-must-be-at-least-256-bits-long-32-bytes!";
    private final UUID userId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private String hashedSecretPassword;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4); // fast for tests
        hashedSecretPassword = passwordEncoder.encode("SecretPass123!");

        jwtProperties = new JwtProperties(rawSecret, "amcs-auth-service", 3600);
        jwtTokenProvider = new JwtTokenProvider(jwtProperties, Clock.systemUTC());
        currentUserPort = new SpringSecurityCurrentUserAdapter();

        authService = new AuthenticationApplicationService(
            userAccountRepositoryPort,
            passwordEncoder,
            new PasswordValidator(),
            5,
            900
        );

        AuthenticationController controller = new AuthenticationController(
            authService,
            jwtTokenProvider,
            jwtProperties,
            currentUserPort
        );

        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(
            jwtTokenProvider,
            userAccountRepositoryPort,
            new RestAuthenticationEntryPoint(objectMapper)
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalRestExceptionHandler())
            .addFilters(jwtFilter)
            .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UserAccount createSampleAccount(int tokenVersion, UserAccountStatus status) {
        return new UserAccount(
            userId,
            "alice",
            "alice@univ.edu",
            hashedSecretPassword,
            UserRole.STUDENT,
            Optional.of(studentId),
            Optional.empty(),
            status,
            0,
            Optional.empty(),
            tokenVersion,
            Instant.now(),
            Instant.now()
        );
    }

    // =========================================================================
    // 1. LOGIN ENDPOINT TESTS
    // =========================================================================
    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginTests {

        @Test
        @DisplayName("Successful login with username returns 200 and valid JWT token")
        void loginSuccessWithUsername() throws Exception {
            UserAccount account = createSampleAccount(1, UserAccountStatus.ACTIVE);
            when(userAccountRepositoryPort.findByUsernameOrEmail("alice")).thenReturn(Optional.of(account));

            LoginRequest request = new LoginRequest("alice", "SecretPass123!");

            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@univ.edu"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.studentId").value(studentId.toString()));
        }

        @Test
        @DisplayName("Successful login with email returns 200 and valid JWT token")
        void loginSuccessWithEmail() throws Exception {
            UserAccount account = createSampleAccount(1, UserAccountStatus.ACTIVE);
            when(userAccountRepositoryPort.findByUsernameOrEmail("alice@univ.edu")).thenReturn(Optional.of(account));

            LoginRequest request = new LoginRequest("alice@univ.edu", "SecretPass123!");

            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("alice"));
        }

        @Test
        @DisplayName("Invalid password returns 401 INVALID_CREDENTIALS")
        void loginInvalidPasswordReturns401() throws Exception {
            UserAccount account = createSampleAccount(1, UserAccountStatus.ACTIVE);
            when(userAccountRepositoryPort.findByUsernameOrEmail("alice")).thenReturn(Optional.of(account));

            LoginRequest request = new LoginRequest("alice", "WrongPassword123!");

            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
        }

        @Test
        @DisplayName("Non-existent username returns 401 without user enumeration")
        void loginNonExistentUserReturns401() throws Exception {
            when(userAccountRepositoryPort.findByUsernameOrEmail("ghost")).thenReturn(Optional.empty());

            LoginRequest request = new LoginRequest("ghost", "SomePass123!");

            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }

        @Test
        @DisplayName("Missing credentials in request returns 400 VALIDATION_FAILED")
        void loginMissingFieldsReturns400() throws Exception {
            LoginRequest request = new LoginRequest("", "");

            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }

        @Test
        @DisplayName("Locked account returns 401 ACCOUNT_LOCKED")
        void loginLockedAccountReturns401() throws Exception {
            UserAccount lockedAccount = new UserAccount(
                userId, "alice", "alice@univ.edu", hashedSecretPassword, UserRole.STUDENT,
                Optional.of(studentId), Optional.empty(), UserAccountStatus.LOCKED,
                5, Optional.of(Instant.now().plusSeconds(600)), 1, Instant.now(), Instant.now()
            );
            when(userAccountRepositoryPort.findByUsernameOrEmail("alice")).thenReturn(Optional.of(lockedAccount));

            LoginRequest request = new LoginRequest("alice", "SecretPass123!");

            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
        }

        @Test
        @DisplayName("Suspended account returns 401 ACCOUNT_SUSPENDED")
        void loginSuspendedAccountReturns401() throws Exception {
            UserAccount suspendedAccount = createSampleAccount(1, UserAccountStatus.SUSPENDED);
            when(userAccountRepositoryPort.findByUsernameOrEmail("alice")).thenReturn(Optional.of(suspendedAccount));

            LoginRequest request = new LoginRequest("alice", "SecretPass123!");

            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("ACCOUNT_SUSPENDED"));
        }
    }

    // =========================================================================
    // 2. /ME PROFILE ENDPOINT TESTS
    // =========================================================================
    @Nested
    @DisplayName("GET /api/v1/auth/me")
    class CurrentUserTests {

        @Test
        @DisplayName("Unauthenticated /me request returns 401 UNAUTHORIZED")
        void meUnauthenticatedReturns401() throws Exception {
            mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("Authenticated /me request returns profile of current token subject")
        void meAuthenticatedReturnsProfile() throws Exception {
            UserAccount account = createSampleAccount(1, UserAccountStatus.ACTIVE);
            when(userAccountRepositoryPort.findById(userId)).thenReturn(Optional.of(account));

            // Generate token
            String token = jwtTokenProvider.generateToken(new com.amcs.application.dto.security.AuthenticationResult(
                userId, "alice", "alice@univ.edu", UserRole.STUDENT, Optional.of(studentId), Optional.empty(), 1
            ));

            mockMvc.perform(get("/api/v1/auth/me")
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@univ.edu"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.studentId").value(studentId.toString()));
        }
    }

    // =========================================================================
    // 3. CHANGE PASSWORD ENDPOINT TESTS
    // =========================================================================
    @Nested
    @DisplayName("POST /api/v1/auth/change-password")
    class ChangePasswordTests {

        @Test
        @DisplayName("Unauthenticated change-password returns 401 UNAUTHORIZED")
        void changePasswordUnauthenticatedReturns401() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest("SecretPass123!", "NewSecretPass456!");

            mockMvc.perform(post("/api/v1/auth/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("Authenticated change-password with correct credentials succeeds and increments token version")
        void changePasswordSuccess() throws Exception {
            UserAccount account = createSampleAccount(1, UserAccountStatus.ACTIVE);
            when(userAccountRepositoryPort.findById(userId)).thenReturn(Optional.of(account));

            String token = jwtTokenProvider.generateToken(new com.amcs.application.dto.security.AuthenticationResult(
                userId, "alice", "alice@univ.edu", UserRole.STUDENT, Optional.of(studentId), Optional.empty(), 1
            ));

            ChangePasswordRequest request = new ChangePasswordRequest("SecretPass123!", "NewSecretPass456!");

            mockMvc.perform(post("/api/v1/auth/change-password")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Password changed successfully"));

            verify(userAccountRepositoryPort).updatePassword(eq(userId), any());
            verify(userAccountRepositoryPort).incrementTokenVersion(userId);
        }

        @Test
        @DisplayName("Incorrect current password returns 400 INVALID_PASSWORD")
        void changePasswordIncorrectCurrentReturns400() throws Exception {
            UserAccount account = createSampleAccount(1, UserAccountStatus.ACTIVE);
            when(userAccountRepositoryPort.findById(userId)).thenReturn(Optional.of(account));

            String token = jwtTokenProvider.generateToken(new com.amcs.application.dto.security.AuthenticationResult(
                userId, "alice", "alice@univ.edu", UserRole.STUDENT, Optional.of(studentId), Optional.empty(), 1
            ));

            ChangePasswordRequest request = new ChangePasswordRequest("WrongCurrentPass123!", "NewSecretPass456!");

            mockMvc.perform(post("/api/v1/auth/change-password")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD"))
                .andExpect(jsonPath("$.message").value("Current password does not match"));
        }

        @Test
        @DisplayName("New password same as current password returns 400 PASSWORD_POLICY_VIOLATION")
        void changePasswordSameAsOldReturns400() throws Exception {
            UserAccount account = createSampleAccount(1, UserAccountStatus.ACTIVE);
            when(userAccountRepositoryPort.findById(userId)).thenReturn(Optional.of(account));

            String token = jwtTokenProvider.generateToken(new com.amcs.application.dto.security.AuthenticationResult(
                userId, "alice", "alice@univ.edu", UserRole.STUDENT, Optional.of(studentId), Optional.empty(), 1
            ));

            ChangePasswordRequest request = new ChangePasswordRequest("SecretPass123!", "SecretPass123!");

            mockMvc.perform(post("/api/v1/auth/change-password")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("PASSWORD_POLICY_VIOLATION"));
        }

        @Test
        @DisplayName("After token version increment, old token is rejected and new token is accepted")
        void oldTokenRejectedAfterPasswordChange() throws Exception {
            // Old token issued with version 1
            String oldToken = jwtTokenProvider.generateToken(new com.amcs.application.dto.security.AuthenticationResult(
                userId, "alice", "alice@univ.edu", UserRole.STUDENT, Optional.of(studentId), Optional.empty(), 1
            ));

            // Database now reflects incremented token version 2
            UserAccount accountVersion2 = createSampleAccount(2, UserAccountStatus.ACTIVE);
            when(userAccountRepositoryPort.findById(userId)).thenReturn(Optional.of(accountVersion2));

            // Request with old token (version 1) must be rejected with 401 TOKEN_EXPIRED
            mockMvc.perform(get("/api/v1/auth/me")
                    .header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));

            // Request with new token (version 2) must succeed with 200 OK
            String newToken = jwtTokenProvider.generateToken(new com.amcs.application.dto.security.AuthenticationResult(
                userId, "alice", "alice@univ.edu", UserRole.STUDENT, Optional.of(studentId), Optional.empty(), 2
            ));

            mockMvc.perform(get("/api/v1/auth/me")
                    .header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
        }
    }
}
