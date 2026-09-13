package com.amcs.infrastructure.web.controller;

import com.amcs.application.dto.assignment.CreateFacultyAssignmentRequest;
import com.amcs.application.dto.assignment.FacultyAssignmentResponse;
import com.amcs.application.service.FacultyAssignmentApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/faculty-assignments")
@Tag(name = "Faculty Assignments", description = "Faculty teaching assignments")
public class FacultyAssignmentController {

    private final FacultyAssignmentApplicationService assignmentService;

    public FacultyAssignmentController(FacultyAssignmentApplicationService assignmentService) {
        this.assignmentService = Objects.requireNonNull(assignmentService, "assignmentService");
    }

    @PostMapping
    @Operation(summary = "Assign a faculty member to a subject and section")
    public ResponseEntity<FacultyAssignmentResponse> createAssignment(@Valid @RequestBody CreateFacultyAssignmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assignmentService.createAssignment(request));
    }

    @GetMapping("/faculty/{facultyId}")
    @Operation(summary = "List assignments for a faculty member")
    public ResponseEntity<List<FacultyAssignmentResponse>> listAssignmentsByFaculty(
        @PathVariable UUID facultyId,
        @RequestParam(required = false) UUID periodId
    ) {
        if (periodId != null) {
            return ResponseEntity.ok(assignmentService.listAssignmentsByFacultyAndPeriod(facultyId, periodId));
        }
        return ResponseEntity.ok(assignmentService.listAssignmentsByFaculty(facultyId));
    }
    
    @GetMapping("/section/{sectionId}")
    @Operation(summary = "List assignments for a section")
    public ResponseEntity<List<FacultyAssignmentResponse>> listAssignmentsBySection(@PathVariable UUID sectionId) {
        return ResponseEntity.ok(assignmentService.listAssignmentsBySection(sectionId));
    }
    
    @PatchMapping("/{id}/end")
    @Operation(summary = "End an assignment prematurely")
    public ResponseEntity<FacultyAssignmentResponse> endAssignment(
        @PathVariable UUID id,
        @RequestParam LocalDate endDate
    ) {
        return ResponseEntity.ok(assignmentService.endAssignment(id, endDate));
    }
}
