package com.example.conference_management_system;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.example.conference_management_system.user.UserRepository;

@SpringBootTest(webEnvironment = RANDOM_PORT)
public class AbstractIntegrationTest {
    @Autowired
    private UserRepository userRepository;

    @ServiceConnection
    protected static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(
            "postgres:15.2-alpine")
            .withUsername("test")
            .withPassword("test")
            .withDatabaseName("code_assessment_test");

    @ServiceConnection
    protected static GenericContainer<?> redisContainer = new GenericContainer<>("redis:alpine")
            .withExposedPorts(6379);

    static {
        postgreSQLContainer.start();
        redisContainer.start();
    }

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
    }
}
