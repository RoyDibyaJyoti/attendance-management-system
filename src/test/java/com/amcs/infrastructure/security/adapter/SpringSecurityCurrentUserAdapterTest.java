package com.amcs.infrastructure.security.adapter;

import com.amcs.application.exception.UnauthenticatedException;
import com.amcs.application.port.out.security.AuthenticatedActor;
import com.amcs.application.port.out.security.UserRole;
import com.amcs.infrastructure.security.principal.SecurityUserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpringSecurityCurrentUserAdapterTest {

    private SpringSecurityCurrentUserAdapter adapter;

    private final UUID userId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID facultyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        adapter = new SpringSecurityCurrentUserAdapter();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("1 & 4. Authenticated student maps to correct AuthenticatedActor preserving student ID")
    void shouldMapStudentPrincipalToActor() {
        SecurityUserPrincipal principal = new SecurityUserPrincipal(
            userId, "CS2026-001", "student@univ.edu", UserRole.STUDENT, Optional.of(studentId), Optional.empty(), 1
        );
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            principal, null, principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<AuthenticatedActor> actorOpt = adapter.getCurrentActor();
        assertThat(actorOpt).isPresent();

        AuthenticatedActor actor = actorOpt.get();
        assertThat(actor.userId()).isEqualTo(userId);
        assertThat(actor.username()).isEqualTo("CS2026-001");
        assertThat(actor.isStudent()).isTrue();
        assertThat(actor.studentId()).contains(studentId);
        assertThat(actor.facultyId()).isEmpty();

        assertThat(adapter.requireCurrentActor()).isEqualTo(actor);
    }

    @Test
    @DisplayName("2 & 5. Authenticated faculty maps to correct AuthenticatedActor preserving faculty ID")
    void shouldMapFacultyPrincipalToActor() {
        SecurityUserPrincipal principal = new SecurityUserPrincipal(
            userId, "EMP-8801", "faculty@univ.edu", UserRole.FACULTY, Optional.empty(), Optional.of(facultyId), 1
        );
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            principal, null, principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        AuthenticatedActor actor = adapter.requireCurrentActor();
        assertThat(actor.isFaculty()).isTrue();
        assertThat(actor.facultyId()).contains(facultyId);
        assertThat(actor.studentId()).isEmpty();
    }

    @Test
    @DisplayName("3. Authenticated HOD admin maps to correct AuthenticatedActor")
    void shouldMapAdminPrincipalToActor() {
        SecurityUserPrincipal principal = new SecurityUserPrincipal(
            userId, "admin_user", "admin@univ.edu", UserRole.HOD_ADMIN, Optional.empty(), Optional.empty(), 1
        );
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            principal, null, principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        AuthenticatedActor actor = adapter.requireCurrentActor();
        assertThat(actor.isAdmin()).isTrue();
    }

    @Test
    @DisplayName("6. Missing authentication returns empty and throws UnauthenticatedException on require")
    void shouldHandleMissingAuthentication() {
        SecurityContextHolder.clearContext();

        assertThat(adapter.getCurrentActor()).isEmpty();
        assertThatThrownBy(() -> adapter.requireCurrentActor())
            .isInstanceOf(UnauthenticatedException.class)
            .hasMessageContaining("No authenticated actor");
    }

    @Test
    @DisplayName("7. AnonymousAuthenticationToken is treated as unauthenticated")
    void shouldHandleAnonymousToken() {
        AnonymousAuthenticationToken anonymousToken = new AnonymousAuthenticationToken(
            "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        SecurityContextHolder.getContext().setAuthentication(anonymousToken);

        assertThat(adapter.getCurrentActor()).isEmpty();
        assertThatThrownBy(() -> adapter.requireCurrentActor())
            .isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    @DisplayName("8. Unsupported principal object (raw String) is treated as unauthenticated")
    void shouldHandleUnsupportedPrincipal() {
        UsernamePasswordAuthenticationToken rawAuth = new UsernamePasswordAuthenticationToken(
            "raw-username-string", "credentials", List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(rawAuth);

        assertThat(adapter.getCurrentActor()).isEmpty();
        assertThatThrownBy(() -> adapter.requireCurrentActor())
            .isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    @DisplayName("9. No password, hash, or token is exposed in AuthenticatedActor")
    void shouldNeverExposeCredentialsInActor() {
        SecurityUserPrincipal principal = new SecurityUserPrincipal(
            userId, "alice", "alice@univ.edu", UserRole.STUDENT, Optional.of(studentId), Optional.empty(), 1
        );
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        AuthenticatedActor actor = adapter.requireCurrentActor();

        // AuthenticatedActor record has only (userId, username, role, studentId, facultyId)
        assertThat(actor.toString()).doesNotContain("password");
        assertThat(actor.toString()).doesNotContain("hash");
        assertThat(actor.toString()).doesNotContain("token");
    }
}
