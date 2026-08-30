package com.amcs.infrastructure.persistence.repository;

import com.amcs.infrastructure.persistence.entity.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataUserAccountRepository extends JpaRepository<UserAccountEntity, UUID> {

    Optional<UserAccountEntity> findByUsername(String username);

    Optional<UserAccountEntity> findByEmail(String email);

    @Query("SELECT u FROM UserAccountEntity u WHERE u.username = :identifier OR u.email = :identifier")
    Optional<UserAccountEntity> findByUsernameOrEmail(@Param("identifier") String identifier);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE UserAccountEntity u SET u.passwordHash = :passwordHash, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :id")
    void updatePasswordHash(@Param("id") UUID id, @Param("passwordHash") String passwordHash);

    @Modifying
    @Query("""
        UPDATE UserAccountEntity u
        SET u.failedAttempts = :failedAttempts,
            u.lockedUntil = :lockedUntil,
            u.status = :status,
            u.updatedAt = CURRENT_TIMESTAMP
        WHERE u.id = :id
    """)
    void updateFailedLogin(
        @Param("id") UUID id,
        @Param("failedAttempts") int failedAttempts,
        @Param("lockedUntil") Instant lockedUntil,
        @Param("status") String status
    );

    @Modifying
    @Query("""
        UPDATE UserAccountEntity u
        SET u.failedAttempts = 0,
            u.lockedUntil = NULL,
            u.status = 'ACTIVE',
            u.updatedAt = CURRENT_TIMESTAMP
        WHERE u.id = :id
    """)
    void resetFailedLogin(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE UserAccountEntity u SET u.status = :status, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") String status);

    @Modifying
    @Query("UPDATE UserAccountEntity u SET u.tokenVersion = u.tokenVersion + 1, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :id")
    void incrementTokenVersion(@Param("id") UUID id);
}
