package com.example.conference_management_system.user;

import com.example.conference_management_system.AbstractRepositoryTest;
import com.example.conference_management_system.entity.User;
import com.example.conference_management_system.entity.Role;
import com.example.conference_management_system.role.RoleType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
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

    //findUserByUsernameFetchingRoles()
    @Test
    void shouldFindUserByUsernameIgnoringCase() {
        //Arrange
        User expected = new User("user", "password", "test user", new HashSet<>());

        this.underTest.save(expected);

        //Act & Assert
        this.underTest.findUserByUsernameFetchingRoles("user").ifPresent(actual -> {
            assertThat(actual.getId()).isPositive();
            assertThat(actual.getUsername()).isEqualTo(expected.getUsername());
            assertThat(actual.getPassword()).isEqualTo(expected.getPassword());
            assertThat(actual.getFullName()).isEqualTo(expected.getFullName());
            assertThat(actual.getRoles()).isEqualTo(expected.getRoles());
        });
    }

    //findUserByFullNameFetchingRoles()
    @Test
    void shouldFindUserByFullNameIgnoringCase() {
        //Arrange
        User expected = new User("user", "password", "test user", new HashSet<>());

        this.underTest.save(expected);

        //Act & Assert
        this.underTest.findUserByFullNameFetchingRoles("test user").ifPresent(actual -> {
            assertThat(actual.getId()).isPositive();
            assertThat(actual.getUsername()).isEqualTo(expected.getUsername());
            assertThat(actual.getPassword()).isEqualTo(expected.getPassword());
            assertThat(actual.getFullName()).isEqualTo(expected.getFullName());
            assertThat(actual.getRoles()).isEqualTo(expected.getRoles());
        });
    }
}