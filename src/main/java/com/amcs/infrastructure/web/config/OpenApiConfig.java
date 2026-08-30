package com.amcs.infrastructure.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("AMCS :: Attendance Management and Calculation System API")
                .version("v1.0")
                .description("""
                    Full-stack university attendance management system.
                    
                    **Key Architecture Highlights:**
                    - Pure domain calculation engine isolated from Spring and JPA.
                    - Atomic batch roll-call submission with domain data integrity validation.
                    - Explicit session lifecycle state machine (SCHEDULED -> CONDUCTED / CANCELLED / RESCHEDULED).
                    - Optimistic locking on sessions to prevent lost updates.
                    - Immutable historical attendance policies.
                    - Mathematical shortage analysis and future predictive calculations.
                    - Streaming low-memory XLSX bulk import pipeline with two-phase staging and commit.
                    - Streaming XLSX reporting pipeline implementing RPT-001 through RPT-008.
                    """)
                .contact(new Contact()
                    .name("AMCS Architecture Team")
                    .email("architecture@amcs.university.edu"))
                .license(new License().name("Proprietary").url("https://amcs.university.edu/terms")));
    }
}
