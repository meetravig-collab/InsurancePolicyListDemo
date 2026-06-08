package com.insurance.dashboard;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for tests that need a real PostgreSQL. Spins up a throwaway PostgreSQL
 * container (Testcontainers) shared across the test run, so the suite is hermetic —
 * it runs identically on any machine and in CI with no manually-provisioned database.
 *
 * <p>{@code @ServiceConnection} auto-overrides the Spring datasource to point at the
 * container; Flyway then builds the schema and seeds it inside the container.
 */
public abstract class AbstractPostgresIT {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        // Singleton container: started once, reused by every test class, reaped at JVM exit.
        POSTGRES.start();
    }
}
