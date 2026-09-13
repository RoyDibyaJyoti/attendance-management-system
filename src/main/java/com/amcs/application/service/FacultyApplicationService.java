package com.amcs.application.service;

import com.amcs.application.dto.common.PagedResponse;
import com.amcs.application.dto.faculty.CreateFacultyRequest;
import com.amcs.application.dto.faculty.FacultyResponse;
import com.amcs.application.dto.faculty.UpdateFacultyRequest;
import com.amcs.application.exception.DuplicateResourceException;
import com.amcs.application.exception.ResourceNotFoundException;
import com.amcs.application.port.out.DepartmentRepositoryPort;
import com.amcs.application.port.out.FacultyRepositoryPort;
import com.amcs.infrastructure.persistence.entity.FacultyEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FacultyApplicationService {

    private final FacultyRepositoryPort facultyPort;
    private final DepartmentRepositoryPort departmentPort;
    private final com.amcs.application.security.ApplicationAuthorizationService authorizationService;

    public FacultyApplicationService(
        FacultyRepositoryPort facultyPort,
        DepartmentRepositoryPort departmentPort,
        com.amcs.application.security.ApplicationAuthorizationService authorizationService
    ) {
        this.facultyPort = Objects.requireNonNull(facultyPort, "facultyPort");
        this.departmentPort = Objects.requireNonNull(departmentPort, "departmentPort");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService");
    }

    @Transactional
    public FacultyResponse createFaculty(CreateFacultyRequest request) {
        authorizationService.requireAdminOnly("create faculty members");
        departmentPort.findById(request.departmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + request.departmentId()));

        if (facultyPort.findByEmployeeId(request.employeeId().trim()).isPresent()) {
            throw new DuplicateResourceException("Faculty already exists with employee ID: " + request.employeeId());
        }

        UUID facultyId = UUID.randomUUID();
        FacultyEntity entity = new FacultyEntity(
            facultyId,
            request.employeeId().trim(),
            request.name().trim(),
            request.email().trim().toLowerCase(),
            request.departmentId()
        );

        FacultyEntity saved = facultyPort.save(entity);
        return toResponse(saved);
    }

    public FacultyResponse getFacultyById(UUID id) {
        if (authorizationService != null) {
            authorizationService.requireFacultyOrAdmin("view faculty profiles");
        }
        return facultyPort.findById(id)
            .map(this::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with ID: " + id));
    }

    @Transactional
    public FacultyResponse updateFaculty(UUID id, UpdateFacultyRequest request) {
        authorizationService.requireAdminOnly("update faculty members");
        FacultyEntity entity = facultyPort.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with ID: " + id));

        if (request.name() != null && !request.name().isBlank()) {
            entity.setName(request.name().trim());
        }
        if (request.email() != null && !request.email().isBlank()) {
            entity.setEmail(request.email().trim().toLowerCase());
        }

        FacultyEntity saved = facultyPort.save(entity);
        return toResponse(saved);
    }

    @Transactional
    public FacultyResponse setFacultyActiveStatus(UUID id, boolean isActive) {
        authorizationService.requireAdminOnly("deactivate/reactivate faculty members");
        FacultyEntity entity = facultyPort.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with ID: " + id));

        entity.setActive(isActive);
        FacultyEntity saved = facultyPort.save(entity);
        return toResponse(saved);
    }

    public PagedResponse<FacultyResponse> listFaculty(int page, int size, boolean includeInactive) {
        if (authorizationService != null) {
            authorizationService.requireFacultyOrAdmin("list faculty members");
        }
        List<FacultyResponse> all = facultyPort.findAll().stream()
            .filter(f -> includeInactive || f.isActive())
            .map(this::toResponse)
            .toList();
        return PagedResponse.of(all, page, size);
    }

    private FacultyResponse toResponse(FacultyEntity entity) {
        return new FacultyResponse(
            entity.getId(),
            entity.getEmployeeId(),
            entity.getName(),
            entity.getEmail(),
            entity.getDepartmentId(),
            entity.isActive()
        );
    }
}
