package com.amcs.application.port.out.security;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Framework-independent application domain actor representing the authenticated user.
 * 100% pure Java; contains no Spring Security, JPA, or JWT dependencies.
 */
public record AuthenticatedActor(
    UUID userId,
    String username,
    UserRole role,
    Optional<UUID> studentId,
    Optional<UUID> facultyId
) {
    public AuthenticatedActor {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(studentId, "studentId must not be null");
        Objects.requireNonNull(facultyId, "facultyId must not be null");
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
