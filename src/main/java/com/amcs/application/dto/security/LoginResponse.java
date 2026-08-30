package com.amcs.application.dto.security;

import java.util.UUID;

public record LoginResponse(
    String token,
    String tokenType,
    long expiresInSeconds,
    UUID userId,
    String username,
    String email,
    String role,
    UUID studentId,
    UUID facultyId
) {}
