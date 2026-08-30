package com.amcs.infrastructure.web.controller;

import com.amcs.application.dto.academic.AcademicPeriodResponse;
import com.amcs.application.dto.academic.CreateAcademicPeriodRequest;
import com.amcs.application.dto.academic.CreateDepartmentRequest;
import com.amcs.application.dto.academic.CreateSectionRequest;
import com.amcs.application.dto.academic.CreateSubjectRequest;
import com.amcs.application.dto.academic.DepartmentResponse;
import com.amcs.application.dto.academic.SectionResponse;
import com.amcs.application.dto.academic.SubjectResponse;
import com.amcs.application.dto.common.PagedResponse;
import com.amcs.application.service.AcademicStructureApplicationService;
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
@RequestMapping("/api/v1/academic")
@Tag(name = "Academic Structure", description = "Departments, academic periods, sections, and curriculum subjects")
public class AcademicStructureController {

    private final AcademicStructureApplicationService academicService;

    public AcademicStructureController(AcademicStructureApplicationService academicService) {
        this.academicService = Objects.requireNonNull(academicService, "academicService");
    }

    // Departments
    @PostMapping("/departments")
    @Operation(summary = "Create an academic department")
    public ResponseEntity<DepartmentResponse> createDepartment(@Valid @RequestBody CreateDepartmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(academicService.createDepartment(request));
    }

    @GetMapping("/departments")
    @Operation(summary = "List all departments")
    public ResponseEntity<List<DepartmentResponse>> listDepartments() {
        return ResponseEntity.ok(academicService.listDepartments());
    }

    // Academic Periods
    @PostMapping("/periods")
    @Operation(summary = "Create an academic period / semester")
    public ResponseEntity<AcademicPeriodResponse> createAcademicPeriod(@Valid @RequestBody CreateAcademicPeriodRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(academicService.createAcademicPeriod(request));
    }

    @GetMapping("/periods")
    @Operation(summary = "List all academic periods")
    public ResponseEntity<List<AcademicPeriodResponse>> listAcademicPeriods() {
        return ResponseEntity.ok(academicService.listAcademicPeriods());
    }

    // Sections
    @PostMapping("/sections")
    @Operation(summary = "Create a class section")
    public ResponseEntity<SectionResponse> createSection(@Valid @RequestBody CreateSectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(academicService.createSection(request));
    }

    @GetMapping("/sections")
    @Operation(summary = "List sections for an academic period")
    public ResponseEntity<List<SectionResponse>> listSections(@RequestParam UUID periodId) {
        return ResponseEntity.ok(academicService.listSectionsByPeriod(periodId));
    }

    // Subjects
    @PostMapping("/subjects")
    @Operation(summary = "Create a curriculum subject")
    public ResponseEntity<SubjectResponse> createSubject(@Valid @RequestBody CreateSubjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(academicService.createSubject(request));
    }

    @GetMapping("/subjects")
    @Operation(summary = "List curriculum subjects with pagination")
    public ResponseEntity<PagedResponse<SubjectResponse>> listSubjects(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        return ResponseEntity.ok(academicService.listSubjects(page, boundedSize));
    }
}
