package com.example.conference_management_system;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import com.example.conference_management_system.config.JpaAuditingConfig;

/*
    We have to import the JpaAuditingConfig class for the database to auto-generated the created date.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        JpaAuditingConfig.class
})
public class AbstractRepositoryTest {

    @ServiceConnection
    protected static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(
            "postgres:15.2-alpine")
            .withUsername("test")
            .withPassword("test")
            .withDatabaseName("conference_ms_test");

    @ServiceConnection
    protected static GenericContainer<?> redisContainer = new GenericContainer<>("redis:alpine")
            .withExposedPorts(6379);

    static {
        postgreSQLContainer.start();
        redisContainer.start();
    }
}