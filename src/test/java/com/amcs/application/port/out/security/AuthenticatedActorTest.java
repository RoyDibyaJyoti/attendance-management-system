package com.amcs.application.port.out.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticatedActorTest {

    private final UUID userId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID facultyId = UUID.randomUUID();

    @Test
    @DisplayName("isStudent() returns true for STUDENT role and verifies persona linkage")
    void shouldVerifyStudentActor() {
        AuthenticatedActor actor = new AuthenticatedActor(
            userId, "CS2026-001", UserRole.STUDENT, Optional.of(studentId), Optional.empty()
        );

        assertThat(actor.isStudent()).isTrue();
        assertThat(actor.isFaculty()).isFalse();
        assertThat(actor.isAdmin()).isFalse();
        assertThat(actor.studentId()).contains(studentId);
        assertThat(actor.facultyId()).isEmpty();
    }

    @Test
    @DisplayName("isFaculty() returns true for FACULTY role and verifies persona linkage")
    void shouldVerifyFacultyActor() {
        AuthenticatedActor actor = new AuthenticatedActor(
            userId, "EMP-8801", UserRole.FACULTY, Optional.empty(), Optional.of(facultyId)
        );

        assertThat(actor.isStudent()).isFalse();
        assertThat(actor.isFaculty()).isTrue();
        assertThat(actor.isAdmin()).isFalse();
        assertThat(actor.studentId()).isEmpty();
        assertThat(actor.facultyId()).contains(facultyId);
    }

    @Test
    @DisplayName("isAdmin() returns true for HOD_ADMIN role")
    void shouldVerifyAdminActor() {
        AuthenticatedActor actor = new AuthenticatedActor(
            userId, "hod_admin", UserRole.HOD_ADMIN, Optional.empty(), Optional.empty()
        );

        assertThat(actor.isStudent()).isFalse();
        assertThat(actor.isFaculty()).isFalse();
        assertThat(actor.isAdmin()).isTrue();
        assertThat(actor.studentId()).isEmpty();
        assertThat(actor.facultyId()).isEmpty();
    }

    @Test
    @DisplayName("Rejects null mandatory fields during construction")
    void shouldRejectNullMandatoryFields() {
        assertThatThrownBy(() -> new AuthenticatedActor(null, "user", UserRole.STUDENT, Optional.empty(), Optional.empty()))
            .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new AuthenticatedActor(userId, null, UserRole.STUDENT, Optional.empty(), Optional.empty()))
            .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new AuthenticatedActor(userId, "user", null, Optional.empty(), Optional.empty()))
            .isInstanceOf(NullPointerException.class);
    }
}
