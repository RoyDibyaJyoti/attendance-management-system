package com.amcs.infrastructure.web.controller;

import com.amcs.application.dto.common.PagedResponse;
import com.amcs.application.dto.student.CreateStudentRequest;
import com.amcs.application.dto.student.StudentResponse;
import com.amcs.application.dto.student.UpdateStudentRequest;
import com.amcs.application.service.StudentApplicationService;
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

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/students")
@Tag(name = "Students", description = "Student registration and profile management APIs")
public class StudentController {

    private final StudentApplicationService studentService;

    public StudentController(StudentApplicationService studentService) {
        this.studentService = Objects.requireNonNull(studentService, "studentService");
    }

    @PostMapping
    @Operation(summary = "Register a new student")
    public ResponseEntity<StudentResponse> createStudent(@Valid @RequestBody CreateStudentRequest request) {
        StudentResponse response = studentService.createStudent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get student profile by UUID")
    public ResponseEntity<StudentResponse> getStudentById(@PathVariable UUID id) {
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    @GetMapping("/registration/{regNo}")
    @Operation(summary = "Get student profile by registration number")
    public ResponseEntity<StudentResponse> getStudentByRegistrationNumber(@PathVariable String regNo) {
        return ResponseEntity.ok(studentService.getStudentByRegistrationNumber(regNo));
    }

    @GetMapping
    @Operation(summary = "List students with pagination")
    public ResponseEntity<PagedResponse<StudentResponse>> listStudents(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "false") boolean includeInactive
    ) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        return ResponseEntity.ok(studentService.listStudents(page, boundedSize, includeInactive));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update student contact information")
    public ResponseEntity<StudentResponse> updateStudent(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateStudentRequest request
    ) {
        return ResponseEntity.ok(studentService.updateStudent(id, request));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a student")
    public ResponseEntity<StudentResponse> deactivateStudent(@PathVariable UUID id) {
        return ResponseEntity.ok(studentService.setStudentActiveStatus(id, false));
    }

    @PatchMapping("/{id}/reactivate")
    @Operation(summary = "Reactivate a student")
    public ResponseEntity<StudentResponse> reactivateStudent(@PathVariable UUID id) {
        return ResponseEntity.ok(studentService.setStudentActiveStatus(id, true));
    }
}
