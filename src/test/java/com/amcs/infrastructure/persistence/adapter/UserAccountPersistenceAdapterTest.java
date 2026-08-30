package com.amcs.infrastructure.persistence.adapter;

import com.amcs.application.port.out.security.UserAccount;
import com.amcs.application.port.out.security.UserAccountStatus;
import com.amcs.application.port.out.security.UserRole;
import com.amcs.infrastructure.persistence.entity.FacultyEntity;
import com.amcs.infrastructure.persistence.entity.StudentEntity;
import com.amcs.infrastructure.persistence.entity.UserAccountEntity;
import com.amcs.infrastructure.persistence.mapper.UserAccountPersistenceMapper;
import com.amcs.infrastructure.persistence.repository.SpringDataFacultyRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataStudentRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataUserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountPersistenceAdapterTest {

    @Mock private SpringDataUserAccountRepository userRepository;
    @Mock private SpringDataStudentRepository studentRepository;
    @Mock private SpringDataFacultyRepository facultyRepository;

    private UserAccountPersistenceMapper mapper;
    private UserAccountPersistenceAdapter adapter;

    private final UUID userId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mapper = new UserAccountPersistenceMapper();
        adapter = new UserAccountPersistenceAdapter(userRepository, mapper, studentRepository, facultyRepository);
    }

    @Test
    @DisplayName("Should save student user account correctly")
    void shouldSaveUserAccount() {
        UserAccount account = new UserAccount(
            userId,
            "CS2026-001",
            "alice@univ.edu",
            "$2a$12$hashedPassword",
            UserRole.STUDENT,
            Optional.of(studentId),
            Optional.empty(),
            UserAccountStatus.ACTIVE,
            0,
            Optional.empty(),
            1,
            Instant.now(),
            Instant.now()
        );

        StudentEntity studentEntity = new StudentEntity(studentId, "CS2026-001", "Alice", "alice@univ.edu", UUID.randomUUID());
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(studentEntity));
        when(userRepository.save(any(UserAccountEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UserAccount saved = adapter.save(account);

        assertThat(saved).isNotNull();
        assertThat(saved.id()).isEqualTo(userId);
        assertThat(saved.username()).isEqualTo("CS2026-001");
        assertThat(saved.role()).isEqualTo(UserRole.STUDENT);
        assertThat(saved.studentId()).contains(studentId);
        assertThat(saved.tokenVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should find user account by username")
    void shouldFindByUsername() {
        UserAccountEntity entity = new UserAccountEntity(
            userId, "CS2026-001", "alice@univ.edu", "hash", "STUDENT", null, null, "ACTIVE", 0, null, 1);

        when(userRepository.findByUsername("CS2026-001")).thenReturn(Optional.of(entity));

        Optional<UserAccount> result = adapter.findByUsername("CS2026-001");

        assertThat(result).isPresent();
        assertThat(result.get().email()).isEqualTo("alice@univ.edu");
    }

    @Test
    @DisplayName("Should update password hash")
    void shouldUpdatePassword() {
        adapter.updatePassword(userId, "$2a$12$newHash");
        verify(userRepository).updatePasswordHash(userId, "$2a$12$newHash");
    }

    @Test
    @DisplayName("Should record failed login and lockout")
    void shouldRecordFailedLogin() {
        Instant lockout = Instant.now().plusSeconds(900);
        adapter.recordFailedLogin(userId, 5, lockout, UserAccountStatus.LOCKED);
        verify(userRepository).updateFailedLogin(userId, 5, lockout, "LOCKED");
    }

    @Test
    @DisplayName("Should reset failed login counter")
    void shouldResetFailedLogin() {
        adapter.resetFailedLogin(userId);
        verify(userRepository).resetFailedLogin(userId);
    }

    @Test
    @DisplayName("Should increment token version")
    void shouldIncrementTokenVersion() {
        adapter.incrementTokenVersion(userId);
        verify(userRepository).incrementTokenVersion(userId);
    }
}
