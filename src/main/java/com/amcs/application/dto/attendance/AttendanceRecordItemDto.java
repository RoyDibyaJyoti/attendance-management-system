package com.amcs.application.dto.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record AttendanceRecordItemDto(
    @NotNull(message = "Student ID must not be null")
    UUID studentId,

    @NotBlank(message = "Attendance status must not be blank")
    @Pattern(regexp = "PRESENT|ABSENT|DUTY_LEAVE|MEDICAL_LEAVE|ON_DUTY",
             message = "Status must be PRESENT, ABSENT, DUTY_LEAVE, MEDICAL_LEAVE, or ON_DUTY")
    String status
) {}
