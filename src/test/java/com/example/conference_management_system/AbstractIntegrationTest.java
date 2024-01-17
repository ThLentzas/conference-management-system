package com.example.conference_management_system;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import com.example.conference_management_system.paper.PaperRepository;
import com.example.conference_management_system.user.UserRepository;
import com.example.conference_management_system.conference.ConferenceRepository;

import java.nio.file.Path;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
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

    /*
        Non-static @TempDir with @BeforeEach: The temporary directory is created for each test method, and the  system
        property is set each time. However, the Spring context has already been initialized before the first
        test method runs, then changing the system property in subsequent methods won't  reconfigure beans that are
        already created and configured, like our FileService which gets a value from the papers.directory property.
        The beans would continue using the value of papers.directory that was resolved when they were first created.
        The value at the time of the creation is still C://papers

        Static @TempDir with @BeforeAll: The temporary directory is created once before any tests run, and the system
        property is set before the Spring context initializes. That way all the beans get the correct property value
        right from the start.
    */
    @TempDir
    static Path tempDirectory;

    @BeforeAll
    static void setupTempDirectory() {
        System.setProperty("papers.directory", tempDirectory.toString());
    }
}
