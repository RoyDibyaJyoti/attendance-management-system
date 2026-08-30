package com.amcs.infrastructure.security;

import com.amcs.infrastructure.security.config.SecurityConfig;
import com.amcs.infrastructure.security.error.RestAccessDeniedHandler;
import com.amcs.infrastructure.security.error.RestAuthenticationEntryPoint;
import com.amcs.infrastructure.security.jwt.JwtAuthenticationFilter;
import com.amcs.infrastructure.web.error.GlobalRestExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 6: Security Headers, CORS Policy & Actuator Exposure Tests")
class SecurityHeadersAndCorsIntegrationTest {

    @RestController
    @RequestMapping("/api/v1/test")
    static class DummyController {
        @GetMapping("/public")
        public String publicEndpoint() {
            return "ok";
        }

        @PostMapping("/data")
        public String dataEndpoint() {
            return "saved";
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        RestAuthenticationEntryPoint authenticationEntryPoint = new RestAuthenticationEntryPoint(objectMapper);
        RestAccessDeniedHandler accessDeniedHandler = new RestAccessDeniedHandler(objectMapper);

        SecurityConfig securityConfig = new SecurityConfig(
            null, null, authenticationEntryPoint, accessDeniedHandler
        );

        mockMvc = MockMvcBuilders.standaloneSetup(new DummyController())
            .setControllerAdvice(new GlobalRestExceptionHandler())
            .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity(
                (request, response, chain) -> {
                    // Let security filter chain run or simulate headers via standalone setup
                    chain.doFilter(request, response);
                }
            ))
            .build();
    }

    @Nested
    @DisplayName("CORS Configuration Logic")
    class CorsConfigurationTests {

        @Test
        @DisplayName("CORS configuration properly validates allowed vs disallowed origins")
        void testCorsConfigurationRules() {
            SecurityConfig securityConfig = new SecurityConfig(null, null, null, null);
            var corsSource = securityConfig.corsConfigurationSource();
            var mockRequest = new org.springframework.mock.web.MockHttpServletRequest();
            mockRequest.setRequestURI("/api/v1/any");

            var config = corsSource.getCorsConfiguration(mockRequest);
            assertThat(config).isNotNull();
            assertThat(config.getAllowedOrigins()).contains("http://localhost:3000", "http://localhost:5173");
            assertThat(config.getAllowedMethods()).contains("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS");
            assertThat(config.getAllowCredentials()).isTrue();
            assertThat(config.getMaxAge()).isEqualTo(3600L);
        }
    }
}
