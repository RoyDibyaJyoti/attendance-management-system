package com.amcs.application.port.out.security;

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

    boolean isFacultyAssigned(UUID facultyId, UUID subjectId, UUID sectionId, UUID academicPeriodId);

    boolean existsAssignment(UUID facultyId, UUID subjectId, UUID sectionId, UUID academicPeriodId);
}
