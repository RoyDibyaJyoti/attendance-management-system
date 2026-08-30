package com.amcs.application.dto.security;

import java.util.UUID;

public record UserProfileResponse(
    UUID userId,
    String username,
    String email,
    String role,
    UUID studentId,
    UUID facultyId
) {}
