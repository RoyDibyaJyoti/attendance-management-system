package com.amcs.application.dto.session;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RescheduleSessionRequest(
    @NotNull(message = "New session date must not be null")
    LocalDate newSessionDate,

    @NotBlank(message = "Rescheduling reason must not be blank")
    @Size(min = 3, max = 255, message = "Reason must be between 3 and 255 characters")
    String reason
) {}
