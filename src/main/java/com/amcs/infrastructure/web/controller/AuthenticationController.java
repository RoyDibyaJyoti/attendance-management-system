package com.amcs.infrastructure.web.controller;

import com.amcs.application.dto.security.ChangePasswordCommand;
import com.amcs.application.dto.security.ChangePasswordRequest;
import com.amcs.application.dto.security.LoginCommand;
import com.amcs.application.dto.security.LoginRequest;
import com.amcs.application.dto.security.LoginResponse;
import com.amcs.application.dto.security.UserProfileResponse;
import com.amcs.application.dto.security.AuthenticationResult;
import com.amcs.application.port.out.security.AuthenticatedActor;
import com.amcs.application.port.out.security.CurrentUserPort;
import com.amcs.application.service.AuthenticationApplicationService;
import com.amcs.infrastructure.security.jwt.JwtProperties;
import com.amcs.infrastructure.security.jwt.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication and user credential management APIs")
public class AuthenticationController {

    private final AuthenticationApplicationService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final CurrentUserPort currentUserPort;

    public AuthenticationController(
        AuthenticationApplicationService authService,
        JwtTokenProvider jwtTokenProvider,
        JwtProperties jwtProperties,
        CurrentUserPort currentUserPort
    ) {
        this.authService = Objects.requireNonNull(authService, "authService");
        this.jwtTokenProvider = Objects.requireNonNull(jwtTokenProvider, "jwtTokenProvider");
        this.jwtProperties = Objects.requireNonNull(jwtProperties, "jwtProperties");
        this.currentUserPort = Objects.requireNonNull(currentUserPort, "currentUserPort");
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and receive a JWT access token")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthenticationResult result = authService.authenticate(
            new LoginCommand(request.usernameOrEmail(), request.password())
        );

        String token = jwtTokenProvider.generateToken(result);
        LoginResponse response = new LoginResponse(
            token,
            "Bearer",
            jwtProperties.getExpirationSeconds(),
            result.userId(),
            result.username(),
            result.email(),
            result.role().name(),
            result.studentId().orElse(null),
            result.facultyId().orElse(null)
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<UserProfileResponse> getCurrentUser() {
        AuthenticatedActor actor = currentUserPort.requireCurrentActor();
        UserProfileResponse profile = authService.getCurrentUserProfile(actor.userId());
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password for the current authenticated user")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        AuthenticatedActor actor = currentUserPort.requireCurrentActor();
        authService.changePassword(
            new ChangePasswordCommand(actor.userId(), request.currentPassword(), request.newPassword())
        );

        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "message", "Password changed successfully"
        ));
    }
}
