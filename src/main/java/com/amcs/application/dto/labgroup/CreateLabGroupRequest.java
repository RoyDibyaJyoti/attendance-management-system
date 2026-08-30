package com.amcs.application.dto.labgroup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateLabGroupRequest(
    @NotBlank(message = "Lab group name must not be blank")
    @Size(min = 1, max = 50, message = "Name must be between 1 and 50 characters")
    String name,

    @NotNull(message = "Section ID must not be null")
    UUID sectionId
) {}
