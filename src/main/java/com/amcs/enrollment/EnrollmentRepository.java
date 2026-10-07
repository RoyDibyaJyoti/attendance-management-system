package com.amcs.enrollment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findBySectionId(Long sectionId);

    boolean existsByStudentIdAndSectionId(Long studentId, Long sectionId);

    Optional<Enrollment> findByStudentIdAndSectionId(Long studentId, Long sectionId);

    /** Returns students for a THEORY class (all enrolled in section) */
    @Query("SELECT e FROM Enrollment e WHERE e.section.id = :sectionId ORDER BY e.student.registrationNumber")
    List<Enrollment> findBySectionIdOrdered(@Param("sectionId") Long sectionId);

    /** Returns students for a LAB class (only those in the specific lab batch) */
    @Query("SELECT e FROM Enrollment e WHERE e.section.id = :sectionId AND e.labBatch.id = :batchId ORDER BY e.student.registrationNumber")
    List<Enrollment> findBySectionIdAndLabBatchId(@Param("sectionId") Long sectionId, @Param("batchId") Long batchId);
}
