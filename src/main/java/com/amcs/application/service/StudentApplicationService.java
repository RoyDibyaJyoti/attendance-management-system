package com.amcs.application.service;

import com.amcs.application.dto.common.PagedResponse;
import com.amcs.application.dto.student.CreateStudentRequest;
import com.amcs.application.dto.student.StudentResponse;
import com.amcs.application.dto.student.UpdateStudentRequest;
import com.amcs.application.exception.DuplicateResourceException;
import com.amcs.application.exception.ResourceNotFoundException;
import com.amcs.application.port.out.DepartmentRepositoryPort;
import com.amcs.application.port.out.StudentRepositoryPort;
import com.amcs.infrastructure.persistence.entity.StudentEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class StudentApplicationService {

    private final StudentRepositoryPort studentPort;
    private final DepartmentRepositoryPort departmentPort;
    private final com.amcs.application.security.ApplicationAuthorizationService authorizationService;

    public StudentApplicationService(
        StudentRepositoryPort studentPort,
        DepartmentRepositoryPort departmentPort
    ) {
        this(studentPort, departmentPort, null);
    }

    public StudentApplicationService(
        StudentRepositoryPort studentPort,
        DepartmentRepositoryPort departmentPort,
        com.amcs.application.security.ApplicationAuthorizationService authorizationService
    ) {
        this.studentPort = Objects.requireNonNull(studentPort, "studentPort");
        this.departmentPort = Objects.requireNonNull(departmentPort, "departmentPort");
        this.authorizationService = authorizationService;
    }

    @Transactional
    public StudentResponse createStudent(CreateStudentRequest request) {
        if (authorizationService != null) {
            authorizationService.requireAdminOnly("create students");
        }
        departmentPort.findById(request.departmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + request.departmentId()));

        if (studentPort.findByRegistrationNumber(request.registrationNumber()).isPresent()) {
            throw new DuplicateResourceException("Student with registration number already exists: " + request.registrationNumber());
        }

        UUID studentId = UUID.randomUUID();
        StudentEntity entity = new StudentEntity(
            studentId,
            request.registrationNumber().trim(),
            request.name().trim(),
            request.email().trim().toLowerCase(),
            request.departmentId()
        );

        StudentEntity saved = studentPort.save(entity);
        return toResponse(saved);
    }

    public StudentResponse getStudentById(UUID id) {
        if (authorizationService != null) {
            authorizationService.requireStudentProfileReadAccess(id);
        }
        return studentPort.findById(id)
            .map(this::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + id));
    }

    public StudentResponse getStudentByRegistrationNumber(String regNo) {
        if (authorizationService != null) {
            authorizationService.requireFacultyOrAdmin("lookup students by registration number");
        }
        return studentPort.findByRegistrationNumber(regNo)
            .map(this::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with registration number: " + regNo));
    }

    public PagedResponse<StudentResponse> listStudents(int page, int size) {
        if (authorizationService != null) {
            authorizationService.requireFacultyOrAdmin("list students");
        }
        List<StudentResponse> all = studentPort.findAll().stream()
            .map(this::toResponse)
            .toList();
        return PagedResponse.of(all, page, size);
    }

    @Transactional
    public StudentResponse updateStudent(UUID id, UpdateStudentRequest request) {
        if (authorizationService != null) {
            authorizationService.requireStudentProfileUpdateAccess(id);
        }
        StudentEntity entity = studentPort.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + id));

        if (request.name() != null && !request.name().isBlank()) {
            entity.setName(request.name().trim());
        }
        if (request.email() != null && !request.email().isBlank()) {
            entity.setEmail(request.email().trim().toLowerCase());
        }

        StudentEntity updated = studentPort.save(entity);
        return toResponse(updated);
    }

    private StudentResponse toResponse(StudentEntity entity) {
        return new StudentResponse(
            entity.getId(),
            entity.getRegistrationNumber(),
            entity.getName(),
            entity.getEmail(),
            entity.getDepartmentId(),
            entity.getCreatedAt()
        );
    }
}
