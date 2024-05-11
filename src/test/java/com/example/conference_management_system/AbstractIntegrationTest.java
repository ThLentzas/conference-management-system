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

/*
    Context caching. By having our integration test extend the same base class we improve the total to run our tests by
    a lot because we only have to start the application context once. We have to provide a clean state for each test.

    https://rieckpil.de/improve-build-times-with-context-caching-from-spring-test/

    Why do we use static containers?

    We want all our IT tests to run in the same database and since we include the entire ApplicationContext, the
    containers will have values before the application context is initialized. Since they are static, they are
    initialized when the class is loaded, which is before any Spring context initialization takes place. Previously we
    had used to @DynamicPropertySource annotation to dynamic add properties to our Environment which is an interface,
    representing the environment in which the current application is running. Once those properties are set, if any bean
    requires values from specific properties, they will be present since they are already added in the environment.
    An example for our use case is the `DataSource` bean that Spring configures for us based on specific properties.
    Those properties are added to Environment and then Spring picks them up and configures the DataSource.
    @ServiceConnection is recommended to be used in newer Spring versions over @DynamicPropertySource.

    https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing.utilities.test-property-values

    Why we don't use @TestContainers and @Container?

    @TestContainer is an annotation for Junit to look at this class for any defined @Containers. We don't let junit
    handle the lifecycle of our containers because we don't want them to be tied to the lifecycle of the class. We want
    all our IT to run against the same database, and we will provide a clean state each time.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(TempDirSetup.class)
public class AbstractIntegrationTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PaperRepository paperRepository;
    @Autowired
    private ConferenceRepository conferenceRepository;

    /*
        Service connections are established by using the image name of the container
        TestContainers handle the port management automatically. They dynamically assign an available port on the host
        to the container's exposed port (the default PostgreSQL port, 5432, in this case
     */
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
