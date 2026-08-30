package com.amcs.application.dto.session;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelSessionRequest(
    @NotBlank(message = "Cancellation reason must not be blank")
    @Size(min = 3, max = 255, message = "Reason must be between 3 and 255 characters")
    String reason
) {}
