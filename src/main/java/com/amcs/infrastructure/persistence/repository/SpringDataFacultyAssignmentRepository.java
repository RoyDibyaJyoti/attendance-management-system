package com.amcs.infrastructure.persistence.repository;

import com.amcs.infrastructure.persistence.entity.FacultyAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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
    
    @Query("SELECT a FROM FacultyAssignmentEntity a WHERE a.section.id = :sectionId")
    List<FacultyAssignmentEntity> findBySectionId(@Param("sectionId") UUID sectionId);

    @Query("""
        SELECT COUNT(a) > 0 FROM FacultyAssignmentEntity a
        WHERE a.faculty.id = :facultyId
          AND a.subject.id = :subjectId
          AND a.section.id = :sectionId
          AND a.academicPeriod.id = :academicPeriodId
          AND a.status = 'ACTIVE'
          AND a.assignmentStart <= :date
          AND (a.assignmentEnd IS NULL OR a.assignmentEnd >= :date)
    """)
    boolean isFacultyAssigned(
        @Param("facultyId") UUID facultyId,
        @Param("subjectId") UUID subjectId,
        @Param("sectionId") UUID sectionId,
        @Param("academicPeriodId") UUID academicPeriodId,
        @Param("date") LocalDate date
    );

    @Query("""
        SELECT a FROM FacultyAssignmentEntity a
        WHERE a.subject.id = :subjectId
          AND a.section.id = :sectionId
          AND a.academicPeriod.id = :academicPeriodId
          AND a.status = 'ACTIVE'
          AND a.assignmentStart <= :endDate
          AND (a.assignmentEnd IS NULL OR a.assignmentEnd >= :startDate)
    """)
    List<FacultyAssignmentEntity> findOverlappingAssignments(
        @Param("subjectId") UUID subjectId,
        @Param("sectionId") UUID sectionId,
        @Param("academicPeriodId") UUID academicPeriodId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    @Query("""
        SELECT a FROM FacultyAssignmentEntity a
        WHERE a.subject.id = :subjectId
          AND a.section.id = :sectionId
          AND a.academicPeriod.id = :academicPeriodId
          AND a.status = 'ACTIVE'
          AND a.assignmentStart <= :endDate
          AND a.assignmentEnd IS NULL
    """)
    List<FacultyAssignmentEntity> findOverlappingAssignmentsOpenEnded(
        @Param("subjectId") UUID subjectId,
        @Param("sectionId") UUID sectionId,
        @Param("academicPeriodId") UUID academicPeriodId,
        @Param("endDate") LocalDate endDate
    );
    
    @Query("""
        SELECT a FROM FacultyAssignmentEntity a
        WHERE a.subject.id = :subjectId
          AND a.section.id = :sectionId
          AND a.academicPeriod.id = :academicPeriodId
          AND a.status = 'ACTIVE'
          AND (a.assignmentEnd IS NULL OR a.assignmentEnd >= :startDate)
    """)
    List<FacultyAssignmentEntity> findOverlappingAssignmentsStartOnly(
        @Param("subjectId") UUID subjectId,
        @Param("sectionId") UUID sectionId,
        @Param("academicPeriodId") UUID academicPeriodId,
        @Param("startDate") LocalDate startDate
    );
    
    @Query("""
        SELECT a FROM FacultyAssignmentEntity a
        WHERE a.subject.id = :subjectId
          AND a.section.id = :sectionId
          AND a.academicPeriod.id = :academicPeriodId
          AND a.status = 'ACTIVE'
    """)
    List<FacultyAssignmentEntity> findOverlappingAssignmentsNoBounds(
        @Param("subjectId") UUID subjectId,
        @Param("sectionId") UUID sectionId,
        @Param("academicPeriodId") UUID academicPeriodId
    );
}
