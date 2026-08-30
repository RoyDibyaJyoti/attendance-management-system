package com.amcs.infrastructure.persistence.adapter;

import com.amcs.application.port.out.security.UserAccount;
import com.amcs.application.port.out.security.UserAccountRepositoryPort;
import com.amcs.application.port.out.security.UserAccountStatus;
import com.amcs.infrastructure.persistence.entity.FacultyEntity;
import com.amcs.infrastructure.persistence.entity.StudentEntity;
import com.amcs.infrastructure.persistence.entity.UserAccountEntity;
import com.amcs.infrastructure.persistence.mapper.UserAccountPersistenceMapper;
import com.amcs.infrastructure.persistence.repository.SpringDataFacultyRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataStudentRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataUserAccountRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class UserAccountPersistenceAdapter implements UserAccountRepositoryPort {

    private final SpringDataUserAccountRepository userRepository;
    private final UserAccountPersistenceMapper mapper;
    private final SpringDataStudentRepository studentRepository;
    private final SpringDataFacultyRepository facultyRepository;

    public UserAccountPersistenceAdapter(
        SpringDataUserAccountRepository userRepository,
        UserAccountPersistenceMapper mapper,
        SpringDataStudentRepository studentRepository,
        SpringDataFacultyRepository facultyRepository
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.studentRepository = Objects.requireNonNull(studentRepository, "studentRepository");
        this.facultyRepository = Objects.requireNonNull(facultyRepository, "facultyRepository");
    }

    @Override
    @Transactional
    public UserAccount save(UserAccount userAccount) {
        StudentEntity student = null;
        if (userAccount.studentId().isPresent()) {
            student = studentRepository.findById(userAccount.studentId().get()).orElse(null);
        }

        FacultyEntity faculty = null;
        if (userAccount.facultyId().isPresent()) {
            faculty = facultyRepository.findById(userAccount.facultyId().get()).orElse(null);
        }

        UserAccountEntity entity = mapper.toEntity(userAccount, student, faculty);
        UserAccountEntity saved = userRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<UserAccount> findById(UUID id) {
        return userRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return userRepository.findByUsername(username).map(mapper::toDomain);
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return userRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public Optional<UserAccount> findByUsernameOrEmail(String identifier) {
        return userRepository.findByUsernameOrEmail(identifier).map(mapper::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional
    public void updatePassword(UUID userId, String newPasswordHash) {
        userRepository.updatePasswordHash(userId, newPasswordHash);
    }

    @Override
    @Transactional
    public void recordFailedLogin(UUID userId, int failedAttempts, Instant lockedUntil, UserAccountStatus status) {
        userRepository.updateFailedLogin(userId, failedAttempts, lockedUntil, status.name());
    }

    @Override
    @Transactional
    public void resetFailedLogin(UUID userId) {
        userRepository.resetFailedLogin(userId);
    }

    @Override
    @Transactional
    public void updateStatus(UUID userId, UserAccountStatus status) {
        userRepository.updateStatus(userId, status.name());
    }

    @Override
    @Transactional
    public void incrementTokenVersion(UUID userId) {
        userRepository.incrementTokenVersion(userId);
    }
}
