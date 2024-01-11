package com.example.conference_management_system.container;

import com.example.conference_management_system.AbstractRepositoryTest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryTestContainersTest extends AbstractRepositoryTest {

    @Test
    void connectionEstablished() {
        assertThat(postgreSQLContainer.isRunning()).isTrue();
        assertThat(postgreSQLContainer.isCreated()).isTrue();
    }
}
