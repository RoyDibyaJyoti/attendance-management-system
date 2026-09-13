package com.amcs.infrastructure.security;

import com.amcs.infrastructure.persistence.PostgresIntegrationTestBase;
import com.amcs.infrastructure.security.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "AMCS_CORS_ALLOWED_ORIGINS=https://roydibyajyoti.github.io"
})
@DisplayName("Production CORS Policy Verification Tests")
class ProductionCorsPolicyIntegrationTest extends PostgresIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityConfig securityConfig;

    @Test
    @DisplayName("CORS Preflight: OPTIONS from production origin https://roydibyajyoti.github.io is accepted")
    void corsPreflightFromProductionOriginSucceeds() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                .header("Origin", "https://roydibyajyoti.github.io")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "https://roydibyajyoti.github.io"))
            .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PATCH,PUT,DELETE,OPTIONS"))
            .andExpect(header().string("Access-Control-Allow-Headers", "Authorization, Content-Type"));
    }

    @Test
    @DisplayName("CORS Preflight: OPTIONS from unauthorized origin receives 403 and no CORS headers")
    void corsPreflightFromUnauthorizedOriginIsRejected() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                .header("Origin", "https://unauthorized.attacker.com")
                .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isForbidden())
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("CORS: Request with subpath in Origin is rejected (CORS requires scheme + host)")
    void corsPreflightWithSubpathInOriginIsRejected() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                .header("Origin", "https://roydibyajyoti.github.io/attendance-management-system/")
                .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isForbidden())
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("Security: Login POST from allowed origin reaches authentication perimeter and is not blocked by CORS")
    void loginPostFromAllowedOriginReachesAuthPerimeter() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .header("Origin", "https://roydibyajyoti.github.io")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usernameOrEmail\":\"nonexistent\",\"password\":\"wrongpassword\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string("Access-Control-Allow-Origin", "https://roydibyajyoti.github.io"));
    }

    @Test
    @DisplayName("CORS Configuration: Verifies no wildcard '*' origin is present in allowed origins")
    void corsConfigDoesNotContainWildcard() {
        var corsSource = securityConfig.corsConfigurationSource();
        var mockRequest = new MockHttpServletRequest();
        mockRequest.setRequestURI("/api/v1/auth/login");

        var config = corsSource.getCorsConfiguration(mockRequest);
        assertThat(config).isNotNull();
        assertThat(config.getAllowedOrigins())
            .containsExactly("https://roydibyajyoti.github.io")
            .doesNotContain("*");
    }
}
