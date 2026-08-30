package com.amcs.infrastructure.web.controller;

import com.amcs.application.dto.common.PagedResponse;
import com.amcs.application.dto.faculty.CreateFacultyRequest;
import com.amcs.application.dto.faculty.FacultyResponse;
import com.amcs.application.service.FacultyApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/faculty")
@Tag(name = "Faculty", description = "Faculty onboarding and profile management APIs")
public class FacultyController {

    private final FacultyApplicationService facultyService;

    public FacultyController(FacultyApplicationService facultyService) {
        this.facultyService = Objects.requireNonNull(facultyService, "facultyService");
    }

    @PostMapping
    @Operation(summary = "Register a new faculty member")
    public ResponseEntity<FacultyResponse> createFaculty(@Valid @RequestBody CreateFacultyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facultyService.createFaculty(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get faculty member by UUID")
    public ResponseEntity<FacultyResponse> getFacultyById(@PathVariable UUID id) {
        return ResponseEntity.ok(facultyService.getFacultyById(id));
    }

    @GetMapping
    @Operation(summary = "List faculty members with pagination")
    public ResponseEntity<PagedResponse<FacultyResponse>> listFaculty(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        return ResponseEntity.ok(facultyService.listFaculty(page, boundedSize));
    }
}
