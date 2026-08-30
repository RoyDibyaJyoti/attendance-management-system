package com.amcs.infrastructure.security.jwt;

import com.amcs.application.dto.security.AuthenticationResult;
import com.amcs.application.port.out.security.UserAccount;
import com.amcs.application.port.out.security.UserAccountRepositoryPort;
import com.amcs.application.port.out.security.UserAccountStatus;
import com.amcs.application.port.out.security.UserRole;
import com.amcs.infrastructure.security.error.RestAuthenticationEntryPoint;
import com.amcs.infrastructure.security.principal.SecurityUserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String TEST_SECRET = "super-secret-key-that-is-at-least-256-bits-long-32-bytes!";
    private static final String ISSUER = "amcs-auth-service";

    @Mock
    private UserAccountRepositoryPort userAccountRepositoryPort;

    @Mock
    private FilterChain filterChain;

    private JwtTokenProvider tokenProvider;
    private RestAuthenticationEntryPoint entryPoint;
    private JwtAuthenticationFilter filter;

    private final UUID userId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID facultyId = UUID.randomUUID();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        Clock fixedClock = Clock.fixed(Instant.parse("2026-08-29T12:00:00Z"), ZoneOffset.UTC);
        JwtProperties properties = new JwtProperties(TEST_SECRET, ISSUER, 3600L);
        tokenProvider = new JwtTokenProvider(properties, fixedClock);
        entryPoint = new RestAuthenticationEntryPoint(objectMapper);
        filter = new JwtAuthenticationFilter(tokenProvider, userAccountRepositoryPort, entryPoint);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UserAccount createActiveAccount(UserRole role, int tokenVersion) {
        return new UserAccount(
            userId,
            "user123",
            "user@univ.edu",
            "hash",
            role,
            Optional.of(studentId),
            Optional.empty(),
            UserAccountStatus.ACTIVE,
            0,
            Optional.empty(),
            tokenVersion,
            Instant.now(),
            Instant.now()
        );
    }

    @Test
    @DisplayName("1. Valid student JWT authenticates and populates SecurityContext with ROLE_STUDENT")
    void shouldAuthenticateValidStudentToken() throws ServletException, IOException {
        AuthenticationResult auth = new AuthenticationResult(
            userId, "CS2026-001", "student@univ.edu", UserRole.STUDENT, Optional.of(studentId), Optional.empty(), 1
        );
        String token = tokenProvider.generateToken(auth);

        UserAccount account = createActiveAccount(UserRole.STUDENT, 1);
        when(userAccountRepositoryPort.findById(userId)).thenReturn(Optional.of(account));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        request.setRequestURI("/api/v1/students/" + studentId + "/attendance/summary");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();

        SecurityUserPrincipal principal = (SecurityUserPrincipal) authentication.getPrincipal();
        assertThat(principal.userId()).isEqualTo(userId);
        assertThat(principal.role()).isEqualTo(UserRole.STUDENT);
        assertThat(principal.studentId()).contains(studentId);
        assertThat(authentication.getAuthorities())
            .extracting("authority")
            .containsExactly("ROLE_STUDENT");

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("2. Valid faculty JWT creates ROLE_FACULTY authority")
    void shouldAuthenticateValidFacultyToken() throws ServletException, IOException {
        AuthenticationResult auth = new AuthenticationResult(
            userId, "EMP-01", "prof@univ.edu", UserRole.FACULTY, Optional.empty(), Optional.of(facultyId), 1
        );
        String token = tokenProvider.generateToken(auth);

        UserAccount account = createActiveAccount(UserRole.FACULTY, 1);
        when(userAccountRepositoryPort.findById(userId)).thenReturn(Optional.of(account));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
            .extracting("authority")
            .containsExactly("ROLE_FACULTY");
    }

    @Test
    @DisplayName("3. Missing Authorization header continues filter chain without authenticating")
    void shouldContinueChainWhenNoAuthHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("4. Malformed JWT returns 401 MALFORMED_TOKEN")
    void shouldReturn401OnMalformedToken() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer not.a.valid.jwt");
        request.setRequestURI("/api/v1/sessions");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("MALFORMED_TOKEN");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("5. Expired JWT returns 401 TOKEN_EXPIRED")
    void shouldReturn401OnExpiredToken() throws ServletException, IOException {
        // Create token with past clock
        Clock pastClock = Clock.fixed(Instant.parse("2026-08-29T10:00:00Z"), ZoneOffset.UTC);
        JwtProperties props = new JwtProperties(TEST_SECRET, ISSUER, 3600L);
        JwtTokenProvider pastProvider = new JwtTokenProvider(props, pastClock);

        AuthenticationResult auth = new AuthenticationResult(
            userId, "user", "u@univ.edu", UserRole.STUDENT, Optional.empty(), Optional.empty(), 1
        );
        String expiredToken = pastProvider.generateToken(auth);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + expiredToken);
        request.setRequestURI("/api/v1/sessions");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("TOKEN_EXPIRED");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("6. Tampered JWT signature returns 401 INVALID_TOKEN")
    void shouldReturn401OnTamperedToken() throws ServletException, IOException {
        AuthenticationResult auth = new AuthenticationResult(
            userId, "user", "u@univ.edu", UserRole.STUDENT, Optional.empty(), Optional.empty(), 1
        );
        String validToken = tokenProvider.generateToken(auth);
        String tamperedToken = validToken.substring(0, validToken.length() - 6) + "xxxxxx";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + tamperedToken);
        request.setRequestURI("/api/v1/sessions");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("INVALID_TOKEN");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("7. Token version mismatch (revoked token) returns 401 TOKEN_EXPIRED")
    void shouldRejectTokenVersionMismatch() throws ServletException, IOException {
        // Token was issued with version 1
        AuthenticationResult auth = new AuthenticationResult(
            userId, "user", "u@univ.edu", UserRole.STUDENT, Optional.empty(), Optional.empty(), 1
        );
        String token = tokenProvider.generateToken(auth);

        // Account was updated (e.g. password changed) so DB has version 2
        UserAccount updatedAccount = createActiveAccount(UserRole.STUDENT, 2);
        when(userAccountRepositoryPort.findById(userId)).thenReturn(Optional.of(updatedAccount));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        request.setRequestURI("/api/v1/sessions");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("TOKEN_EXPIRED");
        assertThat(response.getContentAsString()).contains("revoked or invalidated");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("8. Suspended user account returns 401 ACCOUNT_SUSPENDED")
    void shouldRejectSuspendedAccount() throws ServletException, IOException {
        AuthenticationResult auth = new AuthenticationResult(
            userId, "user", "u@univ.edu", UserRole.STUDENT, Optional.empty(), Optional.empty(), 1
        );
        String token = tokenProvider.generateToken(auth);

        UserAccount suspendedAccount = new UserAccount(
            userId, "user", "u@univ.edu", "hash", UserRole.STUDENT, Optional.empty(), Optional.empty(),
            UserAccountStatus.SUSPENDED, 0, Optional.empty(), 1, Instant.now(), Instant.now()
        );
        when(userAccountRepositoryPort.findById(userId)).thenReturn(Optional.of(suspendedAccount));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("ACCOUNT_SUSPENDED");
        verify(filterChain, never()).doFilter(request, response);
    }
}
