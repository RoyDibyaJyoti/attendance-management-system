package com.amcs.application.dto.academic;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;

public record UpdateSubjectRequest(
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    String name,
    
    @Size(min = 1, max = 40, message = "Course type must be between 1 and 40 characters")
    String courseType,
    
    @Min(value = 0, message = "Credit hours must be at least 0")
    Integer creditHours
) {}
