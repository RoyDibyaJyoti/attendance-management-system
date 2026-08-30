package com.amcs.application.service;

import com.amcs.application.dto.security.AuthenticationResult;
import com.amcs.application.dto.security.ChangePasswordCommand;
import com.amcs.application.dto.security.LoginCommand;
import com.amcs.application.exception.AuthenticationFailedException;
import com.amcs.application.exception.AuthenticationFailedException.FailureReason;
import com.amcs.application.exception.InvalidPasswordException;
import com.amcs.application.exception.PasswordValidationException;
import com.amcs.application.port.out.security.UserAccount;
import com.amcs.application.port.out.security.UserAccountRepositoryPort;
import com.amcs.application.port.out.security.UserAccountStatus;
import com.amcs.application.port.out.security.UserRole;
import com.amcs.infrastructure.security.config.SecurityCryptoConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationApplicationServiceTest {

    @Mock
    private UserAccountRepositoryPort userAccountRepositoryPort;

    private PasswordEncoder passwordEncoder;
    private PasswordValidator passwordValidator;
    private AuthenticationApplicationService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID facultyId = UUID.randomUUID();
    private final String rawPassword = "ValidPassword123!";
    private String encodedPassword;

    @BeforeEach
    void setUp() {
        // Use cost factor 4 in tests for sub-second execution speed
        passwordEncoder = new SecurityCryptoConfig().passwordEncoder(4);
        passwordValidator = new PasswordValidator(8, 128, true, true, true, true);
        encodedPassword = passwordEncoder.encode(rawPassword);

        service = new AuthenticationApplicationService(
            userAccountRepositoryPort,
            passwordEncoder,
            passwordValidator,
            5,
            900
        );
    }

    private UserAccount createTestAccount(
        UserRole role,
        Optional<UUID> sId,
        Optional<UUID> fId,
        UserAccountStatus status,
        int failedAttempts,
        Optional<Instant> lockedUntil
    ) {
        return new UserAccount(
            userId,
            "user123",
            "user@univ.edu",
            encodedPassword,
            role,
            sId,
            fId,
            status,
            failedAttempts,
            lockedUntil,
            1,
            Instant.now(),
            Instant.now()
        );
    }

    @Test
    @DisplayName("1. Successful login using username")
    void shouldLoginSuccessfullyWithUsername() {
        UserAccount account = createTestAccount(UserRole.STUDENT, Optional.of(studentId), Optional.empty(), UserAccountStatus.ACTIVE, 0, Optional.empty());
        when(userAccountRepositoryPort.findByUsernameOrEmail("user123")).thenReturn(Optional.of(account));

        AuthenticationResult result = service.authenticate(new LoginCommand("user123", rawPassword));

        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.username()).isEqualTo("user123");
        assertThat(result.role()).isEqualTo(UserRole.STUDENT);
        assertThat(result.studentId()).contains(studentId);
    }

    @Test
    @DisplayName("2. Successful login using email")
    void shouldLoginSuccessfullyWithEmail() {
        UserAccount account = createTestAccount(UserRole.FACULTY, Optional.empty(), Optional.of(facultyId), UserAccountStatus.ACTIVE, 0, Optional.empty());
        when(userAccountRepositoryPort.findByUsernameOrEmail("user@univ.edu")).thenReturn(Optional.of(account));

        AuthenticationResult result = service.authenticate(new LoginCommand("user@univ.edu", rawPassword));

        assertThat(result).isNotNull();
        assertThat(result.role()).isEqualTo(UserRole.FACULTY);
        assertThat(result.facultyId()).contains(facultyId);
    }

    @Test
    @DisplayName("3. Invalid username or non-existent account returns generic INVALID_CREDENTIALS")
    void shouldRejectNonExistentAccount() {
        when(userAccountRepositoryPort.findByUsernameOrEmail("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authenticate(new LoginCommand("unknown", "anyPass")))
            .isInstanceOf(AuthenticationFailedException.class)
            .hasMessage("Invalid username or password")
            .extracting(e -> ((AuthenticationFailedException) e).getReason())
            .isEqualTo(FailureReason.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("4 & 5. Invalid password increments failed attempt counter")
    void shouldIncrementFailedAttemptsOnWrongPassword() {
        UserAccount account = createTestAccount(UserRole.STUDENT, Optional.of(studentId), Optional.empty(), UserAccountStatus.ACTIVE, 2, Optional.empty());
        when(userAccountRepositoryPort.findByUsernameOrEmail("user123")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.authenticate(new LoginCommand("user123", "WrongPass123!")))
            .isInstanceOf(AuthenticationFailedException.class)
            .hasMessage("Invalid username or password")
            .extracting(e -> ((AuthenticationFailedException) e).getReason())
            .isEqualTo(FailureReason.INVALID_CREDENTIALS);

        verify(userAccountRepositoryPort).recordFailedLogin(eq(userId), eq(3), eq(null), eq(UserAccountStatus.ACTIVE));
    }

    @Test
    @DisplayName("6. Account locks exactly on the fifth consecutive failed attempt")
    void shouldLockAccountOnFifthFailedAttempt() {
        UserAccount account = createTestAccount(UserRole.STUDENT, Optional.of(studentId), Optional.empty(), UserAccountStatus.ACTIVE, 4, Optional.empty());
        when(userAccountRepositoryPort.findByUsernameOrEmail("user123")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.authenticate(new LoginCommand("user123", "WrongPass123!")))
            .isInstanceOf(AuthenticationFailedException.class)
            .extracting(e -> ((AuthenticationFailedException) e).getReason())
            .isEqualTo(FailureReason.INVALID_CREDENTIALS);

        verify(userAccountRepositoryPort).recordFailedLogin(eq(userId), eq(5), any(Instant.class), eq(UserAccountStatus.LOCKED));
    }

    @Test
    @DisplayName("7 & 11. Sixth login attempt while locked is rejected with ACCOUNT_LOCKED")
    void shouldRejectLoginWhenAccountIsLocked() {
        Instant lockExpiration = Instant.now().plusSeconds(600);
        UserAccount account = createTestAccount(UserRole.STUDENT, Optional.of(studentId), Optional.empty(), UserAccountStatus.LOCKED, 5, Optional.of(lockExpiration));
        when(userAccountRepositoryPort.findByUsernameOrEmail("user123")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.authenticate(new LoginCommand("user123", rawPassword)))
            .isInstanceOf(AuthenticationFailedException.class)
            .hasMessageContaining("temporarily locked")
            .extracting(e -> ((AuthenticationFailedException) e).getReason())
            .isEqualTo(FailureReason.ACCOUNT_LOCKED);

        verify(userAccountRepositoryPort, never()).resetFailedLogin(any());
    }

    @Test
    @DisplayName("8. Successful login resets failed attempts")
    void shouldResetFailedAttemptsOnSuccess() {
        UserAccount account = createTestAccount(UserRole.STUDENT, Optional.of(studentId), Optional.empty(), UserAccountStatus.ACTIVE, 3, Optional.empty());
        when(userAccountRepositoryPort.findByUsernameOrEmail("user123")).thenReturn(Optional.of(account));

        AuthenticationResult result = service.authenticate(new LoginCommand("user123", rawPassword));

        assertThat(result).isNotNull();
        verify(userAccountRepositoryPort).resetFailedLogin(userId);
    }

    @Test
    @DisplayName("9. SUSPENDED account cannot authenticate")
    void shouldRejectSuspendedAccount() {
        UserAccount account = createTestAccount(UserRole.STUDENT, Optional.of(studentId), Optional.empty(), UserAccountStatus.SUSPENDED, 0, Optional.empty());
        when(userAccountRepositoryPort.findByUsernameOrEmail("user123")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.authenticate(new LoginCommand("user123", rawPassword)))
            .isInstanceOf(AuthenticationFailedException.class)
            .hasMessage("Account is suspended")
            .extracting(e -> ((AuthenticationFailedException) e).getReason())
            .isEqualTo(FailureReason.ACCOUNT_SUSPENDED);
    }

    @Test
    @DisplayName("10. DEACTIVATED account cannot authenticate")
    void shouldRejectDeactivatedAccount() {
        UserAccount account = createTestAccount(UserRole.STUDENT, Optional.of(studentId), Optional.empty(), UserAccountStatus.DEACTIVATED, 0, Optional.empty());
        when(userAccountRepositoryPort.findByUsernameOrEmail("user123")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.authenticate(new LoginCommand("user123", rawPassword)))
            .isInstanceOf(AuthenticationFailedException.class)
            .hasMessage("Account is deactivated")
            .extracting(e -> ((AuthenticationFailedException) e).getReason())
            .isEqualTo(FailureReason.ACCOUNT_DEACTIVATED);
    }

    @Test
    @DisplayName("12, 13, 14 & 15. HOD_ADMIN can authenticate without student linkage")
    void shouldAuthenticateAdminWithoutStudentLinkage() {
        UserAccount adminAccount = createTestAccount(UserRole.HOD_ADMIN, Optional.empty(), Optional.empty(), UserAccountStatus.ACTIVE, 0, Optional.empty());
        when(userAccountRepositoryPort.findByUsernameOrEmail("admin")).thenReturn(Optional.of(adminAccount));

        AuthenticationResult result = service.authenticate(new LoginCommand("admin", rawPassword));

        assertThat(result.isAdmin()).isTrue();
        assertThat(result.studentId()).isEmpty();
        assertThat(result.facultyId()).isEmpty();
    }

    @Test
    @DisplayName("16. Password is never returned in authentication result")
    void shouldNotExposePasswordInResult() {
        UserAccount account = createTestAccount(UserRole.STUDENT, Optional.of(studentId), Optional.empty(), UserAccountStatus.ACTIVE, 0, Optional.empty());
        when(userAccountRepositoryPort.findByUsernameOrEmail("user123")).thenReturn(Optional.of(account));

        AuthenticationResult result = service.authenticate(new LoginCommand("user123", rawPassword));

        // AuthenticationResult record does not even declare a password field
        assertThat(result.toString()).doesNotContain(rawPassword);
        assertThat(result.toString()).doesNotContain(encodedPassword);
    }

    @Test
    @DisplayName("17 & 21. Correct current password allows change and increments token_version")
    void shouldChangePasswordSuccessfully() {
        UserAccount account = createTestAccount(UserRole.STUDENT, Optional.of(studentId), Optional.empty(), UserAccountStatus.ACTIVE, 0, Optional.empty());
        when(userAccountRepositoryPort.findById(userId)).thenReturn(Optional.of(account));

        String newPassword = "NewSecretPassword456!";
        service.changePassword(new ChangePasswordCommand(userId, rawPassword, newPassword));

        verify(userAccountRepositoryPort).updatePassword(eq(userId), any(String.class));
        verify(userAccountRepositoryPort).incrementTokenVersion(userId);
    }

    @Test
    @DisplayName("18. Incorrect current password rejects password change")
    void shouldRejectPasswordChangeWithWrongCurrentPassword() {
        UserAccount account = createTestAccount(UserRole.STUDENT, Optional.of(studentId), Optional.empty(), UserAccountStatus.ACTIVE, 0, Optional.empty());
        when(userAccountRepositoryPort.findById(userId)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.changePassword(new ChangePasswordCommand(userId, "WrongCurrentPass123!", "NewSecretPassword456!")))
            .isInstanceOf(InvalidPasswordException.class)
            .hasMessage("Current password does not match");

        verify(userAccountRepositoryPort, never()).updatePassword(any(), any());
        verify(userAccountRepositoryPort, never()).incrementTokenVersion(any());
    }

    @Test
    @DisplayName("19. Password policy rejects simple passwords without uppercase/digit/symbol")
    void shouldRejectWeakNewPassword() {
        UserAccount account = createTestAccount(UserRole.STUDENT, Optional.of(studentId), Optional.empty(), UserAccountStatus.ACTIVE, 0, Optional.empty());
        when(userAccountRepositoryPort.findById(userId)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.changePassword(new ChangePasswordCommand(userId, rawPassword, "weakpass")))
            .isInstanceOf(PasswordValidationException.class);

        verify(userAccountRepositoryPort, never()).updatePassword(any(), any());
    }

    @Test
    @DisplayName("20. New password identical to current password is rejected")
    void shouldRejectIdenticalNewPassword() {
        UserAccount account = createTestAccount(UserRole.STUDENT, Optional.of(studentId), Optional.empty(), UserAccountStatus.ACTIVE, 0, Optional.empty());
        when(userAccountRepositoryPort.findById(userId)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.changePassword(new ChangePasswordCommand(userId, rawPassword, rawPassword)))
            .isInstanceOf(PasswordValidationException.class)
            .hasMessage("New password must be different from current password");

        verify(userAccountRepositoryPort, never()).updatePassword(any(), any());
    }
}
