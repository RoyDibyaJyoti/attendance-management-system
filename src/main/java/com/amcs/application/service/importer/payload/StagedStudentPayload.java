package com.amcs.application.service.importer.payload;

import java.util.UUID;

public record StagedStudentPayload(
    String registrationNumber,
    String name,
    String email,
    UUID departmentId,
    UUID sectionId
) {}
