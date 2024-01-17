package com.example.conference_management_system;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import com.example.conference_management_system.paper.PaperRepository;
import com.example.conference_management_system.user.UserRepository;
import com.example.conference_management_system.conference.ConferenceRepository;
import com.example.conference_management_system.config.TempDirSetup;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(TempDirSetup.class)
public class AbstractIntegrationTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PaperRepository paperRepository;
    @Autowired
    private ConferenceRepository conferenceRepository;

    @ServiceConnection
    protected static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(
            "postgres:15.2-alpine")
            .withUsername("test")
            .withPassword("test")
            .withDatabaseName("conference_ms_test");

    @ServiceConnection
    protected static final GenericContainer<?> redisContainer = new GenericContainer<>("redis:alpine")
            .withExposedPorts(6379);

    static {
        postgreSQLContainer.start();
        redisContainer.start();
    }

    @BeforeEach
    void setup() {
        this.userRepository.deleteAll();
        this.paperRepository.deleteAll();
        this.conferenceRepository.deleteAll();
    }
}
