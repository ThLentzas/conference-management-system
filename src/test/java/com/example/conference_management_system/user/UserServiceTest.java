package com.example.conference_management_system.user;

import com.example.conference_management_system.entity.Role;
import com.example.conference_management_system.role.RoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Random;
import java.util.Set;

import com.example.conference_management_system.entity.User;
import com.example.conference_management_system.exception.DuplicateResourceException;

import static org.assertj.core.api.Assertions.assertThat;


/*
    An alternative approach that was used in previous projects is to test the repository via the service without mocking
    it.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    private UserService underTest;

    @BeforeEach
    void setup() {
        this.underTest = new UserService(userRepository);
    }

    @Test
    void shouldRegisterUser() {
        //Arrange
        User expected = new User(
                "user",
                "password",
                "test user",
                Set.of(new Role(RoleType.ROLE_PC_MEMBER)));
        when(this.userRepository.existsByUsernameIgnoreCase(any(String.class))).thenReturn(false);
        when(this.userRepository.save(any(User.class))).thenReturn(expected);

        //Act
        User actual = this.underTest.registerUser(expected);

        //Assert
        assertThat(actual.getUsername()).isEqualTo(expected.getUsername());
        assertThat(actual.getFullName()).isEqualTo("test user");
        assertThat(actual.getRoles()).hasSize(1);
        assertThat(actual.getRoles())
                .extracting(Role::getType)
                .containsExactlyInAnyOrder(RoleType.ROLE_PC_MEMBER);
    }

    @Test
    void shouldThrowDuplicateResourceExceptionWhenRegisteringUserWithExistingUsername() {
        //Arrange
        User actual = new User(
                "user",
                "password",
                "test user",
                Set.of(new Role(RoleType.ROLE_PC_MEMBER)));
        when(this.userRepository.existsByUsernameIgnoreCase(any(String.class))).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> this.underTest.registerUser(actual))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("The provided username already exists");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenRegisteringUserWithUsernameThatExceedsMaxLength() {
        //Arrange
        User actual = new User(
                RandomStringUtils.randomAlphanumeric(new Random().nextInt(21) + 21),
                "password",
                "test user",
                Set.of(new Role(RoleType.ROLE_PC_MEMBER)));

        // Act & Assert
        assertThatThrownBy(() -> underTest.validateUser(actual))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid username. Username must not exceed 20 characters");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenRegisteringUserWithFullNameThatExceedsMaxLength() {
        // Arrange
        User actual = new User(
                "user",
                "password",
                RandomStringUtils.randomAlphanumeric(new Random().nextInt(51) + 51),
                Set.of(new Role(RoleType.ROLE_PC_MEMBER)));

        // Act & Assert
        assertThatThrownBy(() -> underTest.validateUser(actual))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid full name. Full name must not exceed 50 characters");
    }

    /*
        We have to provide a password that follows the requirements otherwise the password validation would throw an
        error since it happens before the full name one.
     */
    @ParameterizedTest
    @ValueSource(strings = {"T3st", "T^st"})
    void shouldThrowIllegalArgumentExceptionForInvalidLastname(String fullName) {
        // Arrange
        User actual = new User(
                "user",
                "password",
                fullName,
                Set.of(new Role(RoleType.ROLE_PC_MEMBER)));

        // Act & Assert
        assertThatThrownBy(() -> underTest.validateUser(actual))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid full name. Full name should contain only characters");
    }
}
