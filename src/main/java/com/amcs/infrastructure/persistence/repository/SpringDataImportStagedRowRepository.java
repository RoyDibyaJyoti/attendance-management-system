package com.amcs.infrastructure.persistence.repository;

import com.amcs.infrastructure.persistence.entity.ImportStagedRowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataImportStagedRowRepository extends JpaRepository<ImportStagedRowEntity, UUID> {

    List<ImportStagedRowEntity> findByJobIdOrderByRowIndexAsc(UUID jobId);

    void deleteByJobId(UUID jobId);

    long countByJobId(UUID jobId);
}
