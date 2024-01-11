package com.example.conference_management_system.user;

import com.example.conference_management_system.AbstractRepositoryTest;
import com.example.conference_management_system.entity.User;
import com.example.conference_management_system.entity.Role;
import com.example.conference_management_system.role.RoleType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryTest extends AbstractRepositoryTest {
    @Autowired
    private UserRepository underTest;

    //existsByUsernameIgnoreCase()
    @Test
    void shouldReturnTrueWhenSearchingForAUserThatExistsWithGivenUsernameIgnoringCase() {
        //Arrange
        User actual = new User("user", "password", "test user", Set.of(new Role(RoleType.ROLE_AUTHOR)));

        //Act
        this.underTest.save(actual);

        //Assert
        assertThat(this.underTest.existsByUsernameIgnoreCase("UsEr")).isTrue();
    }

    @Test
    void shouldReturnFalseWhenSearchingForAUserThatDoesNotExistWithGivenUsernameIgnoringCase() {
        //Arrange
        User actual = new User("user", "password", "test user", Set.of(new Role(RoleType.ROLE_AUTHOR)));

        //Act
        this.underTest.save(actual);

        //Assert
        assertThat(this.underTest.existsByUsernameIgnoreCase("test")).isFalse();
    }
}