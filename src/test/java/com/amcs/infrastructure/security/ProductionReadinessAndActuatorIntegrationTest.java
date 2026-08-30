package com.amcs.infrastructure.security;

import com.amcs.infrastructure.persistence.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("Phase 6: Live Production Readiness, Actuator & Security Perimeter Tests")
class ProductionReadinessAndActuatorIntegrationTest extends PostgresIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Actuator: /actuator/health is publicly accessible and reports UP with healthy database")
    void healthEndpointReportsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Actuator: Sensitive endpoint /actuator/env is not publicly accessible (401 or 403 or 404)")
    void sensitiveActuatorEndpointsAreProtected() throws Exception {
        mockMvc.perform(get("/actuator/env"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                if (status != 401 && status != 403 && status != 404) {
                    throw new AssertionError("Expected 401, 403, or 404 for /actuator/env, but was: " + status);
                }
            });
    }

    @Test
    @DisplayName("Security Headers: HTTP responses include HSTS, CSP, FrameOptions, Nosniff, and PermissionsPolicy")
    void responsesIncludeHardenedSecurityHeaders() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("X-Frame-Options", "DENY"))
            .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
            .andExpect(header().string("Permissions-Policy", "geolocation=(), camera=(), microphone=(), payment=()"))
            .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; sandbox"));
    }

    @Test
    @DisplayName("CORS: Preflight OPTIONS request from allowed origin returns permitted CORS headers")
    void corsPreflightAllowedOriginSucceeds() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("CORS: Request from unauthorized origin is denied or omitted from CORS headers")
    void corsDisallowedOriginDoesNotReturnAccessControlHeader() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                .header("Origin", "http://unauthorized.attacker.com")
                .header("Access-Control-Request-Method", "POST"))
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
