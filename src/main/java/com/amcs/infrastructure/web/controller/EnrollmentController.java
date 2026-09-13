package com.amcs.infrastructure.web.controller;

import com.amcs.application.dto.enrollment.EnrollStudentRequest;
import com.amcs.application.dto.enrollment.EnrollmentResponse;
import com.amcs.application.dto.enrollment.TransferStudentRequest;
import com.amcs.application.service.EnrollmentApplicationService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/enrollments")
@Tag(name = "Enrollments", description = "Student section enrollments, transfers, and history")
public class EnrollmentController {

    private final EnrollmentApplicationService enrollmentService;

    public EnrollmentController(EnrollmentApplicationService enrollmentService) {
        this.enrollmentService = Objects.requireNonNull(enrollmentService, "enrollmentService");
    }

    @PostMapping
    @Operation(summary = "Enroll a student into a section")
    public ResponseEntity<EnrollmentResponse> enrollStudent(@Valid @RequestBody EnrollStudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollmentService.enrollStudent(request));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer a student to a new section (ends old, starts new)")
    public ResponseEntity<EnrollmentResponse> transferStudent(@Valid @RequestBody TransferStudentRequest request) {
        return ResponseEntity.ok(enrollmentService.transferStudent(request));
    }

    @GetMapping("/students/{studentId}")
    @Operation(summary = "Get historical enrollments for a student")
    public ResponseEntity<List<EnrollmentResponse>> getStudentEnrollments(@PathVariable UUID studentId) {
        return ResponseEntity.ok(enrollmentService.getStudentEnrollmentHistory(studentId));
    }

    @GetMapping("/sections/{sectionId}")
    @Operation(summary = "Get enrollments for a section")
    public ResponseEntity<List<EnrollmentResponse>> getSectionEnrollments(@PathVariable UUID sectionId) {
        return ResponseEntity.ok(enrollmentService.getSectionEnrollments(sectionId));
    }
}
