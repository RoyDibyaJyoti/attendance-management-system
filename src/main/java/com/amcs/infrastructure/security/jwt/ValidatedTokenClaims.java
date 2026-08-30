package com.amcs.infrastructure.security.jwt;

import com.amcs.application.port.out.security.UserRole;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Immutable trusted claims extracted after full cryptographic and structural token validation.
 */
public record ValidatedTokenClaims(
    UUID userId,
    String username,
    String email,
    UserRole role,
    Optional<UUID> studentId,
    Optional<UUID> facultyId,
    int tokenVersion,
    Instant issuedAt,
    Instant expiration
) {
    public ValidatedTokenClaims {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(studentId, "studentId must not be null");
        Objects.requireNonNull(facultyId, "facultyId must not be null");
        Objects.requireNonNull(issuedAt, "issuedAt must not be null");
        Objects.requireNonNull(expiration, "expiration must not be null");
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
