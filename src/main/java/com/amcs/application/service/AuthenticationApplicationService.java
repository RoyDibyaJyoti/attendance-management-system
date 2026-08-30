package com.amcs.application.service;

import com.amcs.application.dto.security.AuthenticationResult;
import com.amcs.application.dto.security.ChangePasswordCommand;
import com.amcs.application.dto.security.LoginCommand;
import com.amcs.application.exception.AuthenticationFailedException;
import com.amcs.application.exception.AuthenticationFailedException.FailureReason;
import com.amcs.application.exception.InvalidPasswordException;
import com.amcs.application.exception.PasswordValidationException;
import com.amcs.application.exception.ResourceNotFoundException;
import com.amcs.application.port.out.security.UserAccount;
import com.amcs.application.port.out.security.UserAccountRepositoryPort;
import com.amcs.application.port.out.security.UserAccountStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Application service managing authentication, login tracking, account lockout, and password change.
 * Completely decoupled from Spring Security filter chains and web controllers.
 */
@Service
@Transactional
public class AuthenticationApplicationService {

    private final UserAccountRepositoryPort userAccountRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;
    private final int maxFailedAttempts;
    private final long lockDurationSeconds;

    public AuthenticationApplicationService(
        UserAccountRepositoryPort userAccountRepositoryPort,
        PasswordEncoder passwordEncoder,
        PasswordValidator passwordValidator,
        @Value("${amcs.security.lockout.max-failed-attempts:5}") int maxFailedAttempts,
        @Value("${amcs.security.lockout.duration-seconds:900}") long lockDurationSeconds
    ) {
        this.userAccountRepositoryPort = Objects.requireNonNull(userAccountRepositoryPort, "userAccountRepositoryPort");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder");
        this.passwordValidator = Objects.requireNonNull(passwordValidator, "passwordValidator");
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockDurationSeconds = lockDurationSeconds;
    }

    /**
     * Authenticates a user by username or email and plaintext password.
     * Enforces account status, lockout windows, and failed login tracking.
     */
    public AuthenticationResult authenticate(LoginCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        UserAccount account = userAccountRepositoryPort.findByUsernameOrEmail(command.usernameOrEmail())
            .orElseThrow(() -> new AuthenticationFailedException(
                "Invalid username or password",
                FailureReason.INVALID_CREDENTIALS
            ));

        // Enforce account lifecycle status
        if (account.status() == UserAccountStatus.DEACTIVATED) {
            throw new AuthenticationFailedException("Account is deactivated", FailureReason.ACCOUNT_DEACTIVATED);
        }
        if (account.status() == UserAccountStatus.SUSPENDED) {
            throw new AuthenticationFailedException("Account is suspended", FailureReason.ACCOUNT_SUSPENDED);
        }
        if (account.status() == UserAccountStatus.LOCKED) {
            Instant now = Instant.now();
            if (account.lockedUntil().isPresent() && now.isBefore(account.lockedUntil().get())) {
                throw new AuthenticationFailedException(
                    "Account is temporarily locked due to consecutive failed login attempts",
                    FailureReason.ACCOUNT_LOCKED
                );
            }
        }

        // Verify credentials
        if (passwordEncoder.matches(command.password(), account.passwordHash())) {
            // Reset failed login counter on successful authentication
            if (account.failedAttempts() > 0 || account.status() == UserAccountStatus.LOCKED) {
                userAccountRepositoryPort.resetFailedLogin(account.id());
            }
            return new AuthenticationResult(
                account.id(),
                account.username(),
                account.email(),
                account.role(),
                account.studentId(),
                account.facultyId(),
                account.tokenVersion()
            );
        }

        // Failed password handling: track attempt and lock if threshold reached
        int newFailedAttempts = account.failedAttempts() + 1;
        if (newFailedAttempts >= maxFailedAttempts) {
            Instant lockedUntil = Instant.now().plusSeconds(lockDurationSeconds);
            userAccountRepositoryPort.recordFailedLogin(
                account.id(),
                newFailedAttempts,
                lockedUntil,
                UserAccountStatus.LOCKED
            );
        } else {
            userAccountRepositoryPort.recordFailedLogin(
                account.id(),
                newFailedAttempts,
                null,
                account.status()
            );
        }

        // Always return generic message to avoid credential enumeration
        throw new AuthenticationFailedException("Invalid username or password", FailureReason.INVALID_CREDENTIALS);
    }

    /**
     * Changes a user password after verifying the current password and policy requirements.
     * Increments token_version to invalidate prior JWTs.
     */
    public void changePassword(ChangePasswordCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        UserAccount account = userAccountRepositoryPort.findById(command.userId())
            .orElseThrow(() -> new ResourceNotFoundException("User account not found: " + command.userId()));

        if (!passwordEncoder.matches(command.currentPassword(), account.passwordHash())) {
            throw new InvalidPasswordException("Current password does not match");
        }

        passwordValidator.validate(command.newPassword());

        if (passwordEncoder.matches(command.newPassword(), account.passwordHash())) {
            throw new PasswordValidationException("New password must be different from current password");
        }

        String newPasswordHash = passwordEncoder.encode(command.newPassword());
        userAccountRepositoryPort.updatePassword(account.id(), newPasswordHash);
        userAccountRepositoryPort.incrementTokenVersion(account.id());
    }

    /**
     * Retrieves the profile information for the authenticated user account.
     */
    @Transactional(readOnly = true)
    public com.amcs.application.dto.security.UserProfileResponse getCurrentUserProfile(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        UserAccount account = userAccountRepositoryPort.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User account not found: " + userId));

        return new com.amcs.application.dto.security.UserProfileResponse(
            account.id(),
            account.username(),
            account.email(),
            account.role().name(),
            account.studentId().orElse(null),
            account.facultyId().orElse(null)
        );
    }
}
