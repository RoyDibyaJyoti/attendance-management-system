package com.amcs.infrastructure.web.controller;

import com.amcs.application.dto.common.PagedResponse;
import com.amcs.application.dto.student.CreateStudentRequest;
import com.amcs.application.dto.student.StudentResponse;
import com.amcs.application.dto.student.UpdateStudentRequest;
import com.amcs.application.exception.DuplicateResourceException;
import com.amcs.application.exception.ResourceNotFoundException;
import com.amcs.application.service.StudentApplicationService;
import com.amcs.infrastructure.web.error.GlobalRestExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StudentControllerTest {

    @Mock private StudentApplicationService studentService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final UUID studentId = UUID.randomUUID();
    private final UUID deptId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        StudentController controller = new StudentController(studentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalRestExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("POST /api/v1/students returns 201 Created on valid input")
    void shouldCreateStudent() throws Exception {
        CreateStudentRequest request = new CreateStudentRequest("CS2026-001", "Bob Smith", "bob@univ.edu", deptId);
        StudentResponse response = new StudentResponse(studentId, "CS2026-001", "Bob Smith", "bob@univ.edu", deptId, Instant.now());

        when(studentService.createStudent(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(studentId.toString()))
            .andExpect(jsonPath("$.registrationNumber").value("CS2026-001"));
    }

    @Test
    @DisplayName("POST /api/v1/students returns 409 Conflict when registration number exists")
    void shouldReturn409OnDuplicateStudent() throws Exception {
        CreateStudentRequest request = new CreateStudentRequest("CS2026-001", "Bob Smith", "bob@univ.edu", deptId);

        when(studentService.createStudent(any()))
            .thenThrow(new DuplicateResourceException("Student already exists"));

        mockMvc.perform(post("/api/v1/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    @DisplayName("POST /api/v1/students returns 400 Bad Request when validation fails")
    void shouldReturn400OnInvalidEmail() throws Exception {
        // Invalid email format
        CreateStudentRequest invalidRequest = new CreateStudentRequest("CS2026-001", "Bob", "not-an-email", deptId);

        mockMvc.perform(post("/api/v1/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("GET /api/v1/students/{id} returns 200 OK")
    void shouldGetStudentById() throws Exception {
        StudentResponse response = new StudentResponse(studentId, "CS2026-001", "Bob Smith", "bob@univ.edu", deptId, Instant.now());

        when(studentService.getStudentById(studentId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/students/{id}", studentId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Bob Smith"));
    }

    @Test
    @DisplayName("GET /api/v1/students/{id} returns 404 when not found")
    void shouldReturn404WhenStudentNotFound() throws Exception {
        when(studentService.getStudentById(studentId))
            .thenThrow(new ResourceNotFoundException("Student not found"));

        mockMvc.perform(get("/api/v1/students/{id}", studentId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("PATCH /api/v1/students/{id} updates contact info")
    void shouldUpdateStudent() throws Exception {
        UpdateStudentRequest request = new UpdateStudentRequest("Bob Johnson", "bobj@univ.edu");
        StudentResponse response = new StudentResponse(studentId, "CS2026-001", "Bob Johnson", "bobj@univ.edu", deptId, Instant.now());

        when(studentService.updateStudent(eq(studentId), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/students/{id}", studentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Bob Johnson"));
    }
}
