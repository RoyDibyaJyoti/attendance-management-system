package com.amcs.lab;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LabBatchRepository extends JpaRepository<LabBatch, Long> {
    List<LabBatch> findBySectionId(Long sectionId);
    Optional<LabBatch> findByNameAndSectionId(String name, Long sectionId);
    boolean existsByNameAndSectionId(String name, Long sectionId);
}
