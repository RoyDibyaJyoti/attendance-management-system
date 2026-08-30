package com.amcs.application.service;

import com.amcs.application.dto.labgroup.AssignLabGroupRequest;
import com.amcs.application.dto.labgroup.CreateLabGroupRequest;
import com.amcs.application.dto.labgroup.LabGroupMembershipResponse;
import com.amcs.application.dto.labgroup.LabGroupResponse;
import com.amcs.application.exception.InvalidBusinessOperationException;
import com.amcs.application.exception.ResourceNotFoundException;
import com.amcs.application.port.out.LabGroupRepositoryPort;
import com.amcs.application.port.out.SectionRepositoryPort;
import com.amcs.application.port.out.StudentRepositoryPort;
import com.amcs.infrastructure.persistence.entity.LabGroupEntity;
import com.amcs.infrastructure.persistence.entity.LabGroupMembershipEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class LabGroupApplicationService {

    private final LabGroupRepositoryPort labGroupPort;
    private final SectionRepositoryPort sectionPort;
    private final StudentRepositoryPort studentPort;
    private final com.amcs.application.security.ApplicationAuthorizationService authorizationService;

    public LabGroupApplicationService(
        LabGroupRepositoryPort labGroupPort,
        SectionRepositoryPort sectionPort,
        StudentRepositoryPort studentPort,
        com.amcs.application.security.ApplicationAuthorizationService authorizationService
    ) {
        this.labGroupPort = Objects.requireNonNull(labGroupPort, "labGroupPort");
        this.sectionPort = Objects.requireNonNull(sectionPort, "sectionPort");
        this.studentPort = Objects.requireNonNull(studentPort, "studentPort");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService");
    }

    @Transactional
    public LabGroupResponse createLabGroup(CreateLabGroupRequest request) {
        authorizationService.requireAdminOnly("create lab groups");
        sectionPort.findById(request.sectionId())
            .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + request.sectionId()));

        LabGroupEntity entity = new LabGroupEntity(UUID.randomUUID(), request.name().trim(), request.sectionId());
        LabGroupEntity saved = labGroupPort.saveGroup(entity);
        return new LabGroupResponse(saved.getId(), saved.getName(), saved.getSectionId());
    }

    @Transactional
    public LabGroupMembershipResponse assignStudentToLabGroup(AssignLabGroupRequest request) {
        if (authorizationService != null) {
            authorizationService.requireAdminOnly("assign students to lab groups");
        }
        studentPort.findById(request.studentId())
            .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + request.studentId()));
        labGroupPort.findGroupById(request.labGroupId())
            .orElseThrow(() -> new ResourceNotFoundException("Lab group not found: " + request.labGroupId()));

        if (request.effectiveEnd() != null && request.effectiveEnd().isBefore(request.effectiveStart())) {
            throw new InvalidBusinessOperationException("Lab group membership end date cannot precede start date");
        }

        LabGroupMembershipEntity entity = new LabGroupMembershipEntity(
            UUID.randomUUID(),
            request.studentId(),
            request.labGroupId(),
            request.effectiveStart(),
            request.effectiveEnd()
        );
        LabGroupMembershipEntity saved = labGroupPort.saveMembership(entity);

        return new LabGroupMembershipResponse(
            saved.getId(), saved.getStudentId(), saved.getLabGroupId(), saved.getEffectiveStart(), saved.getEffectiveEnd());
    }

    public List<LabGroupResponse> listLabGroupsBySection(UUID sectionId) {
        return labGroupPort.findGroupsBySection(sectionId).stream()
            .map(g -> new LabGroupResponse(g.getId(), g.getName(), g.getSectionId()))
            .toList();
    }
}
