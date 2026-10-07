package com.amcs.section;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SectionRepository extends JpaRepository<Section, Long> {
    List<Section> findByTermId(Long termId);
    List<Section> findByDepartmentId(Long departmentId);
    Optional<Section> findByNameAndDepartmentIdAndTermId(String name, Long departmentId, Long termId);
    List<Section> findByName(String name);
}
