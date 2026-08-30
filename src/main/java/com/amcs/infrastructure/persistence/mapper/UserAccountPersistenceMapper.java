package com.amcs.infrastructure.persistence.mapper;

import com.amcs.application.port.out.security.UserAccount;
import com.amcs.application.port.out.security.UserAccountStatus;
import com.amcs.application.port.out.security.UserRole;
import com.amcs.infrastructure.persistence.entity.FacultyEntity;
import com.amcs.infrastructure.persistence.entity.StudentEntity;
import com.amcs.infrastructure.persistence.entity.UserAccountEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserAccountPersistenceMapper {

    public UserAccount toDomain(UserAccountEntity entity) {
        if (entity == null) return null;
        return new UserAccount(
            entity.getId(),
            entity.getUsername(),
            entity.getEmail(),
            entity.getPasswordHash(),
            UserRole.valueOf(entity.getRole()),
            Optional.ofNullable(entity.getStudentId()),
            Optional.ofNullable(entity.getFacultyId()),
            UserAccountStatus.valueOf(entity.getStatus()),
            entity.getFailedAttempts(),
            Optional.ofNullable(entity.getLockedUntil()),
            entity.getTokenVersion(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public UserAccountEntity toEntity(UserAccount domain, StudentEntity student, FacultyEntity faculty) {
        if (domain == null) return null;
        return new UserAccountEntity(
            domain.id(),
            domain.username(),
            domain.email(),
            domain.passwordHash(),
            domain.role().name(),
            student,
            faculty,
            domain.status().name(),
            domain.failedAttempts(),
            domain.lockedUntil().orElse(null),
            domain.tokenVersion()
        );
    }
}
