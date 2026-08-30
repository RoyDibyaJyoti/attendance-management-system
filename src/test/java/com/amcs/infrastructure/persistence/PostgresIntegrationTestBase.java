package com.amcs.infrastructure.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

/**
 * Base class for all PostgreSQL persistence integration tests.
 *
 * <p><strong>Testcontainers Isolation:</strong>
 * Uses genuine PostgreSQL 16 Alpine container managed via Testcontainers.
 * H2 is strictly forbidden for persistence integration tests to ensure
 * production-realistic behavior, constraints, and Flyway migration execution.
 *
 * <p>{@code disabledWithoutDocker = true} ensures tests communicate clearly
 * if the host Docker environment is not currently active.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
public abstract class PostgresIntegrationTestBase {

    @Container
    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("amcs_test_db")
        .withUsername("test_user")
        .withPassword("test_password")
        .withReuse(false);

    @DynamicPropertySource
    static void configurePostgresProperties(DynamicPropertyRegistry registry) {
        if (postgres.isRunning()) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        }
    }

    @Autowired
    protected DataSource dataSource;

    @BeforeEach
    void setUpBase() {
        // Individual test setups can override or add cleanup
    }
}
