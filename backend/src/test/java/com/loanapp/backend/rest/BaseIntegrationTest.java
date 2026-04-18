package com.loanapp.backend.rest;


import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;


/**
 * Base class for all integration tests.
 * Spins up a real PostgreSQL via Testcontainers and applies Liquibase migrations automatically.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class BaseIntegrationTest {

    private static final PostgreSQLContainer<?> postgres = PostgresTestContainer.getInstance();

    @DynamicPropertySource
    static void overrideDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}

