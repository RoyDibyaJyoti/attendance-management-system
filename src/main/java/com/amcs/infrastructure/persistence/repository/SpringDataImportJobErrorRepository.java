package com.amcs.infrastructure.persistence.repository;

import com.amcs.infrastructure.persistence.entity.ImportJobErrorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataImportJobErrorRepository extends JpaRepository<ImportJobErrorEntity, UUID> {

    List<ImportJobErrorEntity> findByJobIdOrderByRowIndexAsc(UUID jobId);

    void deleteByJobId(UUID jobId);
}
