package com.example.conference_management_system.user;

import com.example.conference_management_system.AbstractUnitTest;
import com.example.conference_management_system.entity.User;
import com.example.conference_management_system.entity.Role;
import com.example.conference_management_system.role.RoleType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryTest extends AbstractUnitTest {
    @Autowired
    private UserRepository underTest;

    @Test
    void shouldRegisterUser() {
        //Arrange
        User expected = new User("user", "password", "test user", Set.of(new Role(RoleType.ROLE_AUTHOR)));
        expected = this.underTest.save(expected);

        /*
            Act & Assert

            We can't call expected.getUsername() inside the lamda because variables  used in lambda expression should be
            final or effectively final. When registering a user, they will have no roles by default, so we check against
            an empty Set.
         */
        this.underTest.findById(expected.getId()).ifPresent(
                actual -> {
                    assertThat(actual.getId()).isPositive();
                    assertThat(actual.getUsername()).isEqualTo("user");
                    assertThat(actual.getPassword()).isEqualTo("password");
                    assertThat(actual.getFullName()).isEqualTo("test user");
                    assertThat(actual.getRoles())
                            .extracting(Role::getType)
                            .containsExactlyInAnyOrder(RoleType.ROLE_AUTHOR);
                }
        );
    }

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