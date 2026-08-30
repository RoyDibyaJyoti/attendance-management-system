package com.amcs.application.service;

import com.amcs.application.dto.academic.AcademicPeriodResponse;
import com.amcs.application.dto.academic.CreateAcademicPeriodRequest;
import com.amcs.application.dto.academic.CreateDepartmentRequest;
import com.amcs.application.dto.academic.CreateSectionRequest;
import com.amcs.application.dto.academic.CreateSubjectRequest;
import com.amcs.application.dto.academic.DepartmentResponse;
import com.amcs.application.dto.academic.SectionResponse;
import com.amcs.application.dto.academic.SubjectResponse;
import com.amcs.application.dto.common.PagedResponse;
import com.amcs.application.exception.DuplicateResourceException;
import com.amcs.application.exception.InvalidBusinessOperationException;
import com.amcs.application.exception.ResourceNotFoundException;
import com.amcs.application.port.out.AcademicPeriodRepositoryPort;
import com.amcs.application.port.out.DepartmentRepositoryPort;
import com.amcs.application.port.out.SectionRepositoryPort;
import com.amcs.application.port.out.SubjectRepositoryPort;
import com.amcs.domain.academic.AcademicPeriod;
import com.amcs.domain.academic.CourseType;
import com.amcs.domain.academic.Subject;
import com.amcs.infrastructure.persistence.entity.DepartmentEntity;
import com.amcs.infrastructure.persistence.entity.SectionEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AcademicStructureApplicationService {

    private final DepartmentRepositoryPort departmentPort;
    private final AcademicPeriodRepositoryPort periodPort;
    private final SectionRepositoryPort sectionPort;
    private final SubjectRepositoryPort subjectPort;
    private final com.amcs.application.security.ApplicationAuthorizationService authorizationService;

    public AcademicStructureApplicationService(
        DepartmentRepositoryPort departmentPort,
        AcademicPeriodRepositoryPort periodPort,
        SectionRepositoryPort sectionPort,
        SubjectRepositoryPort subjectPort
    ) {
        this(departmentPort, periodPort, sectionPort, subjectPort, null);
    }

    public AcademicStructureApplicationService(
        DepartmentRepositoryPort departmentPort,
        AcademicPeriodRepositoryPort periodPort,
        SectionRepositoryPort sectionPort,
        SubjectRepositoryPort subjectPort,
        com.amcs.application.security.ApplicationAuthorizationService authorizationService
    ) {
        this.departmentPort = Objects.requireNonNull(departmentPort, "departmentPort");
        this.periodPort = Objects.requireNonNull(periodPort, "periodPort");
        this.sectionPort = Objects.requireNonNull(sectionPort, "sectionPort");
        this.subjectPort = Objects.requireNonNull(subjectPort, "subjectPort");
        this.authorizationService = authorizationService;
    }

    // Departments
    @Transactional
    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {
        if (authorizationService != null) {
            authorizationService.requireAdminOnly("create departments");
        }
        if (departmentPort.findByCode(request.code().trim().toUpperCase()).isPresent()) {
            throw new DuplicateResourceException("Department already exists with code: " + request.code());
        }
        DepartmentEntity entity = new DepartmentEntity(UUID.randomUUID(), request.code().trim().toUpperCase(), request.name().trim());
        DepartmentEntity saved = departmentPort.save(entity);
        return new DepartmentResponse(saved.getId(), saved.getCode(), saved.getName());
    }

    public List<DepartmentResponse> listDepartments() {
        return departmentPort.findAll().stream()
            .map(d -> new DepartmentResponse(d.getId(), d.getCode(), d.getName()))
            .toList();
    }

    // Academic Periods
    @Transactional
    public AcademicPeriodResponse createAcademicPeriod(CreateAcademicPeriodRequest request) {
        if (authorizationService != null) {
            authorizationService.requireAdminOnly("create academic periods");
        }
        if (request.endDate().isBefore(request.startDate())) {
            throw new InvalidBusinessOperationException("Academic period end date must be on or after start date");
        }
        if (periodPort.findByName(request.name().trim()).isPresent()) {
            throw new DuplicateResourceException("Academic period already exists with name: " + request.name());
        }
        UUID id = UUID.randomUUID();
        AcademicPeriod domainPeriod = new AcademicPeriod(request.name().trim(), request.startDate(), request.endDate());
        AcademicPeriod saved = periodPort.save(id, domainPeriod);
        return new AcademicPeriodResponse(id, saved.name(), saved.startDate(), saved.endDate());
    }

    public List<AcademicPeriodResponse> listAcademicPeriods() {
        return periodPort.findAll().stream()
            .map(p -> new AcademicPeriodResponse(UUID.randomUUID(), p.name(), p.startDate(), p.endDate()))
            .toList();
    }

    // Sections
    @Transactional
    public SectionResponse createSection(CreateSectionRequest request) {
        if (authorizationService != null) {
            authorizationService.requireAdminOnly("create sections");
        }
        departmentPort.findById(request.departmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + request.departmentId()));
        periodPort.findById(request.academicPeriodId())
            .orElseThrow(() -> new ResourceNotFoundException("Academic period not found: " + request.academicPeriodId()));

        SectionEntity entity = new SectionEntity(
            UUID.randomUUID(), request.name().trim(), request.departmentId(), request.academicPeriodId());
        SectionEntity saved = sectionPort.save(entity);
        return new SectionResponse(saved.getId(), saved.getName(), saved.getDepartmentId(), saved.getAcademicPeriodId());
    }

    public List<SectionResponse> listSectionsByPeriod(UUID academicPeriodId) {
        return sectionPort.findByAcademicPeriod(academicPeriodId).stream()
            .map(s -> new SectionResponse(s.getId(), s.getName(), s.getDepartmentId(), s.getAcademicPeriodId()))
            .toList();
    }

    // Subjects
    @Transactional
    public SubjectResponse createSubject(CreateSubjectRequest request) {
        if (authorizationService != null) {
            authorizationService.requireAdminOnly("create subjects");
        }
        departmentPort.findById(request.departmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + request.departmentId()));

        if (subjectPort.findByCode(request.code().trim().toUpperCase()).isPresent()) {
            throw new DuplicateResourceException("Subject code already exists: " + request.code());
        }

        UUID subjectId = UUID.randomUUID();
        Subject domainSubject = new Subject(
            subjectId,
            request.name().trim(),
            request.code().trim().toUpperCase(),
            CourseType.valueOf(request.courseType()),
            request.creditHours()
        );

        Subject saved = subjectPort.save(domainSubject, request.departmentId());
        return new SubjectResponse(saved.id(), saved.code(), saved.name(), saved.courseType().name(), saved.creditHours());
    }

    public PagedResponse<SubjectResponse> listSubjects(int page, int size) {
        List<SubjectResponse> all = subjectPort.findAll().stream()
            .map(s -> new SubjectResponse(s.id(), s.code(), s.name(), s.courseType().name(), s.creditHours()))
            .toList();
        return PagedResponse.of(all, page, size);
    }
}
