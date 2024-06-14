package com.example.conference_management_system;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;

import com.example.conference_management_system.config.JpaAuditingConfig;

/*
    We have to import the JpaAuditingConfig class for the database to auto-generated the created date.

    Why we don't use @TestContainers and @Container?

    @TestContainer is an annotation for Junit to look at this class for any defined @Containers. We don't let junit
    handle the lifecycle of our containers because we don't want them to be tied to the lifecycle of the class. We want
    all our repository tests to run against the same database, and we provide a clean state each time, since the
    transaction is rolled back by @DataJpaTest.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        JpaAuditingConfig.class
})
public abstract class AbstractRepositoryTest {
    @ServiceConnection
    protected static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(
            "postgres:15.2-alpine")
            .withUsername("test")
            .withPassword("test")
            .withDatabaseName("conference_ms_test");

    static {
        postgreSQLContainer.start();
    }
}