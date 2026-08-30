package com.amcs.application.port.out.security;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound repository port for user accounts and credentials.
 */
public interface UserAccountRepositoryPort {

    UserAccount save(UserAccount userAccount);

    Optional<UserAccount> findById(UUID id);

    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findByUsernameOrEmail(String identifier);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    void updatePassword(UUID userId, String newPasswordHash);

    void recordFailedLogin(UUID userId, int failedAttempts, Instant lockedUntil, UserAccountStatus status);

    void resetFailedLogin(UUID userId);

    void updateStatus(UUID userId, UserAccountStatus status);

    void incrementTokenVersion(UUID userId);
}
