package com.amcs.application.dto.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CorrectAttendanceRecordRequest(
    @NotBlank(message = "New attendance status must not be blank")
    @Pattern(regexp = "PRESENT|ABSENT|DUTY_LEAVE|MEDICAL_LEAVE|ON_DUTY",
             message = "Status must be PRESENT, ABSENT, DUTY_LEAVE, MEDICAL_LEAVE, or ON_DUTY")
    String newStatus,

    @NotBlank(message = "Correction reason must not be blank")
    @Size(min = 5, max = 500, message = "Reason must be between 5 and 500 characters")
    String reason,

    @NotNull(message = "Approver/Faculty ID must not be null")
    UUID approverId
) {}
