package com.amcs.infrastructure.web.security.ratelimit;

import com.amcs.infrastructure.web.error.GlobalRestExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Phase 6: Rate Limiting & DoS Protection Integration Tests")
class RateLimitingIntegrationTest {

    @RestController
    static class RateLimitTestController {
        @PostMapping("/api/v1/auth/login")
        public String login() {
            return "login-processed";
        }

        @PostMapping("/api/v1/imports/STUDENTS")
        public String importData() {
            return "import-processed";
        }

        @GetMapping("/api/v1/reports/rpt-001")
        public String reportData() {
            return "report-processed";
        }

        @GetMapping("/api/v1/academic/departments")
        public String normalApi() {
            return "normal-processed";
        }
    }

    private MockMvc mockMvc;
    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        // Configure with strict limits: 5 auth per minute, 10 heavy per minute
        rateLimitingFilter = new RateLimitingFilter(true, 5, 10, mapper);
        rateLimitingFilter.reset();

        mockMvc = MockMvcBuilders.standaloneSetup(new RateLimitTestController())
            .setControllerAdvice(new GlobalRestExceptionHandler())
            .addFilter(rateLimitingFilter)
            .build();
    }

    @Test
    @DisplayName("Authentication: 5 consecutive attempts pass; 6th attempt is throttled with HTTP 429")
    void authenticationThrottlingTriggers429() throws Exception {
        // 5 allowed attempts
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .header("X-Forwarded-For", "192.168.1.100"))
                .andExpect(status().isOk());
        }

        // 6th attempt must be rejected with 429 and Retry-After header
        mockMvc.perform(post("/api/v1/auth/login")
                .header("X-Forwarded-For", "192.168.1.100"))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"))
            .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"))
            .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    @DisplayName("Heavy Reports: 10 requests pass; 11th request is throttled with HTTP 429")
    void heavyReportThrottlingTriggers429() throws Exception {
        // 10 allowed requests
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/v1/reports/rpt-001")
                    .header("X-Forwarded-For", "192.168.1.200"))
                .andExpect(status().isOk());
        }

        // 11th request must be throttled
        mockMvc.perform(get("/api/v1/reports/rpt-001")
                .header("X-Forwarded-For", "192.168.1.200"))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"))
            .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("Normal API endpoints are unthrottled by the rate limiter")
    void normalEndpointsNotThrottled() throws Exception {
        // 20 requests to non-heavy, non-auth endpoints should all pass
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(get("/api/v1/academic/departments")
                    .header("X-Forwarded-For", "192.168.1.150"))
                .andExpect(status().isOk());
        }
    }
}
