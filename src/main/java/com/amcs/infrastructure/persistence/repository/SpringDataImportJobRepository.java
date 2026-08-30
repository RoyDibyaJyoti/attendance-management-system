package com.amcs.infrastructure.persistence.repository;

import com.amcs.infrastructure.persistence.entity.ImportJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataImportJobRepository extends JpaRepository<ImportJobEntity, UUID> {

    List<ImportJobEntity> findByCreatedByUserIdOrderByCreatedAtDesc(UUID createdByUserId);

    List<ImportJobEntity> findByStatusOrderByCreatedAtDesc(String status);

    List<ImportJobEntity> findByImportTypeOrderByCreatedAtDesc(String importType);
}
