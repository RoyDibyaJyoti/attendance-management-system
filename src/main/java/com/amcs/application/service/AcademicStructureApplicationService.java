package com.amcs.application.service;

import com.amcs.application.dto.academic.AcademicPeriodResponse;
import com.amcs.application.dto.academic.CreateAcademicPeriodRequest;
import com.amcs.application.dto.academic.CreateDepartmentRequest;
import com.amcs.application.dto.academic.CreateSectionRequest;
import com.amcs.application.dto.academic.CreateSubjectRequest;
import com.amcs.application.dto.academic.DepartmentResponse;
import com.amcs.application.dto.academic.SectionResponse;
import com.amcs.application.dto.academic.SubjectResponse;
import com.amcs.application.dto.academic.UpdateDepartmentRequest;
import com.amcs.application.dto.academic.UpdateSectionRequest;
import com.amcs.application.dto.academic.UpdateSubjectRequest;
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
import com.amcs.infrastructure.persistence.entity.AcademicPeriodEntity;
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
        SubjectRepositoryPort subjectPort,
        com.amcs.application.security.ApplicationAuthorizationService authorizationService
    ) {
        this.departmentPort = Objects.requireNonNull(departmentPort, "departmentPort");
        this.periodPort = Objects.requireNonNull(periodPort, "periodPort");
        this.sectionPort = Objects.requireNonNull(sectionPort, "sectionPort");
        this.subjectPort = Objects.requireNonNull(subjectPort, "subjectPort");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService");
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
        return new DepartmentResponse(saved.getId(), saved.getCode(), saved.getName(), saved.isActive());
    }

    @Transactional
    public DepartmentResponse updateDepartment(UUID id, UpdateDepartmentRequest request) {
        if (authorizationService != null) {
            authorizationService.requireAdminOnly("update departments");
        }
        DepartmentEntity entity = departmentPort.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + id));
        entity.setName(request.name().trim());
        DepartmentEntity saved = departmentPort.save(entity);
        return new DepartmentResponse(saved.getId(), saved.getCode(), saved.getName(), saved.isActive());
    }

    @Transactional
    public DepartmentResponse setDepartmentActiveStatus(UUID id, boolean isActive) {
        if (authorizationService != null) {
            authorizationService.requireAdminOnly("deactivate/reactivate departments");
        }
        DepartmentEntity entity = departmentPort.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + id));
        entity.setActive(isActive);
        DepartmentEntity saved = departmentPort.save(entity);
        return new DepartmentResponse(saved.getId(), saved.getCode(), saved.getName(), saved.isActive());
    }

    public List<DepartmentResponse> listDepartments(boolean includeInactive) {
        return departmentPort.findAll().stream()
            .filter(d -> includeInactive || d.isActive())
            .map(d -> new DepartmentResponse(d.getId(), d.getCode(), d.getName(), d.isActive()))
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
        return periodPort.findAllWithIds().stream()
            .map(e -> new AcademicPeriodResponse(e.getId(), e.getName(), e.getStartDate(), e.getEndDate()))
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
        return new SectionResponse(saved.getId(), saved.getName(), saved.getDepartmentId(), saved.getAcademicPeriodId(), saved.isActive());
    }

    @Transactional
    public SectionResponse updateSection(UUID id, UpdateSectionRequest request) {
        if (authorizationService != null) {
            authorizationService.requireAdminOnly("update sections");
        }
        SectionEntity entity = sectionPort.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + id));
        entity.setName(request.name().trim());
        SectionEntity saved = sectionPort.save(entity);
        return new SectionResponse(saved.getId(), saved.getName(), saved.getDepartmentId(), saved.getAcademicPeriodId(), saved.isActive());
    }

    @Transactional
    public SectionResponse setSectionActiveStatus(UUID id, boolean isActive) {
        if (authorizationService != null) {
            authorizationService.requireAdminOnly("deactivate/reactivate sections");
        }
        SectionEntity entity = sectionPort.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + id));
        entity.setActive(isActive);
        SectionEntity saved = sectionPort.save(entity);
        return new SectionResponse(saved.getId(), saved.getName(), saved.getDepartmentId(), saved.getAcademicPeriodId(), saved.isActive());
    }

    public List<SectionResponse> listSectionsByPeriod(UUID academicPeriodId, boolean includeInactive) {
        return sectionPort.findByAcademicPeriod(academicPeriodId).stream()
            .filter(s -> includeInactive || s.isActive())
            .map(s -> new SectionResponse(s.getId(), s.getName(), s.getDepartmentId(), s.getAcademicPeriodId(), s.isActive()))
            .toList();
    }

    public SectionResponse getSection(UUID sectionId) {
        SectionEntity entity = sectionPort.findById(sectionId)
            .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + sectionId));
        return new SectionResponse(entity.getId(), entity.getName(), entity.getDepartmentId(), entity.getAcademicPeriodId(), entity.isActive());
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
            request.creditHours(),
            true
        );

        Subject saved = subjectPort.save(domainSubject, request.departmentId());
        return new SubjectResponse(saved.id(), saved.code(), saved.name(), saved.courseType().name(), saved.creditHours(), saved.isActive());
    }

    @Transactional
    public SubjectResponse updateSubject(UUID id, UpdateSubjectRequest request) {
        if (authorizationService != null) {
            authorizationService.requireAdminOnly("update subjects");
        }
        Subject domainSubject = subjectPort.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + id));
            
        Subject updated = new Subject(
            domainSubject.id(),
            request.name().trim(),
            domainSubject.code(),
            CourseType.valueOf(request.courseType()),
            request.creditHours(),
            domainSubject.isActive()
        );
        
        Subject saved = subjectPort.update(updated); 
        return new SubjectResponse(saved.id(), saved.code(), saved.name(), saved.courseType().name(), saved.creditHours(), saved.isActive());
    }

    @Transactional
    public SubjectResponse setSubjectActiveStatus(UUID id, boolean isActive) {
        if (authorizationService != null) {
            authorizationService.requireAdminOnly("deactivate/reactivate subjects");
        }
        Subject saved = subjectPort.setActiveStatus(id, isActive);
        return new SubjectResponse(saved.id(), saved.code(), saved.name(), saved.courseType().name(), saved.creditHours(), saved.isActive());
    }

    public PagedResponse<SubjectResponse> listSubjects(int page, int size, boolean includeInactive) {
        List<SubjectResponse> all = subjectPort.findAll().stream()
            .filter(s -> includeInactive || s.isActive())
            .map(s -> new SubjectResponse(s.id(), s.code(), s.name(), s.courseType().name(), s.creditHours(), s.isActive()))
            .toList();
        return PagedResponse.of(all, page, size);
    }
}
