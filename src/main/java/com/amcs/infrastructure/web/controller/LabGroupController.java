package com.amcs.infrastructure.web.controller;

import com.amcs.application.dto.labgroup.AssignLabGroupRequest;
import com.amcs.application.dto.labgroup.CreateLabGroupRequest;
import com.amcs.application.dto.labgroup.LabGroupMembershipResponse;
import com.amcs.application.dto.labgroup.LabGroupResponse;
import com.amcs.application.service.LabGroupApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lab-groups")
@Tag(name = "Lab Groups", description = "Laboratory cohort divisions and student assignments")
public class LabGroupController {

    private final LabGroupApplicationService labGroupService;

    public LabGroupController(LabGroupApplicationService labGroupService) {
        this.labGroupService = Objects.requireNonNull(labGroupService, "labGroupService");
    }

    @PostMapping
    @Operation(summary = "Create a lab group for a section")
    public ResponseEntity<LabGroupResponse> createLabGroup(@Valid @RequestBody CreateLabGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(labGroupService.createLabGroup(request));
    }

    @PostMapping("/assign")
    @Operation(summary = "Assign a student to a lab group")
    public ResponseEntity<LabGroupMembershipResponse> assignStudent(@Valid @RequestBody AssignLabGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(labGroupService.assignStudentToLabGroup(request));
    }

    @GetMapping
    @Operation(summary = "List lab groups for a section")
    public ResponseEntity<List<LabGroupResponse>> listLabGroups(@RequestParam UUID sectionId) {
        return ResponseEntity.ok(labGroupService.listLabGroupsBySection(sectionId));
    }
}
