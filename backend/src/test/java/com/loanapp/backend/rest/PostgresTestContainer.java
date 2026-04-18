package com.loanapp.backend.rest;


import org.testcontainers.containers.PostgreSQLContainer;

public class PostgresTestContainer {

    private static final PostgreSQLContainer<?> CONTAINER;

    static {
        CONTAINER = new PostgreSQLContainer<>("postgres:16")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test");
        CONTAINER.start();
    }

    public static PostgreSQLContainer<?> getInstance() {
        return CONTAINER;
    }
}
