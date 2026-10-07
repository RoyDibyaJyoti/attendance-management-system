package com.amcs.auth;

import com.amcs.config.JwtUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepo, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    record LoginResponse(String token, String username, String role, String fullName) {}
    record MeResponse(String username, String role, String fullName) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        User user = userRepo.findByUsername(req.username())
                .orElse(null);
        if (user == null || !user.isActive() || !passwordEncoder.matches(req.password(), user.getPassword())) {
            return ResponseEntity.status(401).body(new ErrorBody("Invalid username or password"));
        }
        String token = jwtUtil.generate(user.getUsername(), user.getRole());
        return ResponseEntity.ok(new LoginResponse(token, user.getUsername(), user.getRole(), user.getFullName()));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal String username) {
        if (username == null) return ResponseEntity.status(401).build();
        return userRepo.findByUsername(username)
                .map(u -> ResponseEntity.ok(new MeResponse(u.getUsername(), u.getRole(), u.getFullName())))
                .orElse(ResponseEntity.status(404).build());
    }

    record ErrorBody(String message) {}
}
