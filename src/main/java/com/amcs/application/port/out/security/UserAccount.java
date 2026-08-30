package com.amcs.application.port.out.security;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Application-facing representation of an authentication user account.
 * Decoupled from JPA entities and Spring Security details.
 */
public record UserAccount(
    UUID id,
    String username,
    String email,
    String passwordHash,
    UserRole role,
    Optional<UUID> studentId,
    Optional<UUID> facultyId,
    UserAccountStatus status,
    int failedAttempts,
    Optional<Instant> lockedUntil,
    int tokenVersion,
    Instant createdAt,
    Instant updatedAt
) {
    public UserAccount {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(studentId, "studentId must not be null");
        Objects.requireNonNull(facultyId, "facultyId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(lockedUntil, "lockedUntil must not be null");
    }

    public boolean isActive() {
        return status == UserAccountStatus.ACTIVE;
    }

    public boolean isLocked() {
        return status == UserAccountStatus.LOCKED;
    }

    public boolean isStudent() {
        return role == UserRole.STUDENT;
    }

    public boolean isFaculty() {
        return role == UserRole.FACULTY;
    }

    public boolean isAdmin() {
        return role == UserRole.HOD_ADMIN;
    }
}
