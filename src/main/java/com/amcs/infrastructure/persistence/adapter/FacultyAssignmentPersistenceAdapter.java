package com.amcs.infrastructure.persistence.adapter;

import com.amcs.application.port.out.security.FacultyAssignment;
import com.amcs.application.port.out.security.FacultyAssignmentRepositoryPort;
import com.amcs.infrastructure.persistence.entity.AcademicPeriodEntity;
import com.amcs.infrastructure.persistence.entity.FacultyAssignmentEntity;
import com.amcs.infrastructure.persistence.entity.FacultyEntity;
import com.amcs.infrastructure.persistence.entity.SectionEntity;
import com.amcs.infrastructure.persistence.entity.SubjectEntity;
import com.amcs.infrastructure.persistence.mapper.FacultyAssignmentPersistenceMapper;
import com.amcs.infrastructure.persistence.repository.SpringDataAcademicPeriodRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataFacultyAssignmentRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataFacultyRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataSectionRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataSubjectRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class FacultyAssignmentPersistenceAdapter implements FacultyAssignmentRepositoryPort {

    private final SpringDataFacultyAssignmentRepository assignmentRepository;
    private final FacultyAssignmentPersistenceMapper mapper;
    private final SpringDataFacultyRepository facultyRepository;
    private final SpringDataSubjectRepository subjectRepository;
    private final SpringDataSectionRepository sectionRepository;
    private final SpringDataAcademicPeriodRepository periodRepository;

    public FacultyAssignmentPersistenceAdapter(
        SpringDataFacultyAssignmentRepository assignmentRepository,
        FacultyAssignmentPersistenceMapper mapper,
        SpringDataFacultyRepository facultyRepository,
        SpringDataSubjectRepository subjectRepository,
        SpringDataSectionRepository sectionRepository,
        SpringDataAcademicPeriodRepository periodRepository
    ) {
        this.assignmentRepository = Objects.requireNonNull(assignmentRepository, "assignmentRepository");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.facultyRepository = Objects.requireNonNull(facultyRepository, "facultyRepository");
        this.subjectRepository = Objects.requireNonNull(subjectRepository, "subjectRepository");
        this.sectionRepository = Objects.requireNonNull(sectionRepository, "sectionRepository");
        this.periodRepository = Objects.requireNonNull(periodRepository, "periodRepository");
    }

    @Override
    @Transactional
    public FacultyAssignment save(FacultyAssignment assignment) {
        FacultyEntity faculty = facultyRepository.findById(assignment.facultyId())
            .orElseThrow(() -> new IllegalArgumentException("Faculty not found: " + assignment.facultyId()));
        SubjectEntity subject = subjectRepository.findById(assignment.subjectId())
            .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + assignment.subjectId()));
        SectionEntity section = sectionRepository.findById(assignment.sectionId())
            .orElseThrow(() -> new IllegalArgumentException("Section not found: " + assignment.sectionId()));
        AcademicPeriodEntity period = periodRepository.findById(assignment.academicPeriodId())
            .orElseThrow(() -> new IllegalArgumentException("Academic period not found: " + assignment.academicPeriodId()));

        FacultyAssignmentEntity entity = mapper.toEntity(assignment, faculty, subject, section, period);
        FacultyAssignmentEntity saved = assignmentRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<FacultyAssignment> findById(UUID id) {
        return assignmentRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<FacultyAssignment> findByFacultyId(UUID facultyId) {
        return assignmentRepository.findByFacultyId(facultyId)
            .stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public List<FacultyAssignment> findByFacultyIdAndAcademicPeriodId(UUID facultyId, UUID academicPeriodId) {
        return assignmentRepository.findByFacultyIdAndAcademicPeriodId(facultyId, academicPeriodId)
            .stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public boolean isFacultyAssigned(UUID facultyId, UUID subjectId, UUID sectionId, UUID academicPeriodId) {
        return assignmentRepository.isFacultyAssigned(facultyId, subjectId, sectionId, academicPeriodId);
    }

    @Override
    public boolean existsAssignment(UUID facultyId, UUID subjectId, UUID sectionId, UUID academicPeriodId) {
        return assignmentRepository.isFacultyAssigned(facultyId, subjectId, sectionId, academicPeriodId);
    }
}
