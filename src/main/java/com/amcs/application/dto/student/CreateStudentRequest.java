package com.amcs.application.dto.student;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateStudentRequest(
    @NotBlank(message = "Registration number must not be blank")
    @Size(min = 2, max = 30, message = "Registration number must be between 2 and 30 characters")
    String registrationNumber,

    @NotBlank(message = "Name must not be blank")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    String name,

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Must be a well-formed email address")
    String email,

    @NotNull(message = "Department ID must not be null")
    UUID departmentId
) {}
