package com.amcs.application.port.out.security;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound repository port for faculty teaching assignments.
 */
public interface FacultyAssignmentRepositoryPort {

    FacultyAssignment save(FacultyAssignment assignment);

    Optional<FacultyAssignment> findById(UUID id);

    List<FacultyAssignment> findByFacultyId(UUID facultyId);

    List<FacultyAssignment> findByFacultyIdAndAcademicPeriodId(UUID facultyId, UUID academicPeriodId);

    List<FacultyAssignment> findBySectionId(UUID sectionId);

    // Get active assignments for a given day
    boolean isFacultyAssigned(UUID facultyId, UUID subjectId, UUID sectionId, UUID academicPeriodId, LocalDate date);

    // Find overlapping assignments for a context
    List<FacultyAssignment> findOverlappingAssignments(UUID subjectId, UUID sectionId, UUID academicPeriodId, LocalDate start, LocalDate end);
}
