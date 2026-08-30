package com.amcs.application.dto.labgroup;

import java.util.UUID;

public record LabGroupResponse(
    UUID id,
    String name,
    UUID sectionId
) {}
