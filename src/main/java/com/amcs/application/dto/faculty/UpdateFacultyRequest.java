package com.amcs.application.dto.faculty;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateFacultyRequest(
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    String name,

    @Email(message = "Must be a well-formed email address")
    String email
) {}
