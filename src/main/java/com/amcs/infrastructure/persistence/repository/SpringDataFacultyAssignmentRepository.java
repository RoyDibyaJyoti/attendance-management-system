package com.amcs.infrastructure.persistence.repository;

import com.amcs.infrastructure.persistence.entity.FacultyAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataFacultyAssignmentRepository extends JpaRepository<FacultyAssignmentEntity, UUID> {

    @Query("SELECT a FROM FacultyAssignmentEntity a WHERE a.faculty.id = :facultyId")
    List<FacultyAssignmentEntity> findByFacultyId(@Param("facultyId") UUID facultyId);

    @Query("SELECT a FROM FacultyAssignmentEntity a WHERE a.faculty.id = :facultyId AND a.academicPeriod.id = :academicPeriodId")
    List<FacultyAssignmentEntity> findByFacultyIdAndAcademicPeriodId(
        @Param("facultyId") UUID facultyId,
        @Param("academicPeriodId") UUID academicPeriodId
    );

    @Query("""
        SELECT COUNT(a) > 0 FROM FacultyAssignmentEntity a
        WHERE a.faculty.id = :facultyId
          AND a.subject.id = :subjectId
          AND a.section.id = :sectionId
          AND a.academicPeriod.id = :academicPeriodId
    """)
    boolean isFacultyAssigned(
        @Param("facultyId") UUID facultyId,
        @Param("subjectId") UUID subjectId,
        @Param("sectionId") UUID sectionId,
        @Param("academicPeriodId") UUID academicPeriodId
    );
}
