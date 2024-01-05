package com.example.conference_management_system.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.conference_management_system.entity.User;
import com.example.conference_management_system.exception.UnauthorizedException;
import com.example.conference_management_system.security.SecurityUser;
import com.example.conference_management_system.user.UserService;
import com.example.conference_management_system.entity.Role;
import com.example.conference_management_system.role.RoleRepository;
import com.example.conference_management_system.role.RoleType;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserService userService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private RoleRepository roleRepository;
    private AuthService underTest;

    @BeforeEach
    void setup() {
        this.underTest = new AuthService(userService, passwordEncoder, authenticationManager, roleRepository);
    }

    @Test
    void shouldRegisterUser() {
        //Arrange
        RegisterRequest registerRequest = new RegisterRequest(
                "user",
                "password",
                "test user",
                Set.of(RoleType.ROLE_AUTHOR));
        Set<Role> roles = registerRequest.roleTypes().stream()
                .map(Role::new)
                .collect(Collectors.toSet());
        User user = new User(registerRequest.username(), registerRequest.password(), registerRequest.fullName(), roles);
        SecurityUser securityUser = new SecurityUser(user);

        Authentication expected = new UsernamePasswordAuthenticationToken(
                securityUser,
                null,
                securityUser.getAuthorities());

        when(this.passwordEncoder.encode(any(String.class))).thenReturn("password");
        when(this.roleRepository.findByType(any(RoleType.class))).thenReturn(Optional.of(new Role(
                RoleType.ROLE_AUTHOR)));
        doNothing().when(this.userService).validateUser(any(User.class));
        doNothing().when(this.userService).registerUser(any(User.class));

        //Act
        Authentication actual = this.underTest.registerUser(registerRequest);

        //Assert
        assertThat(actual).isEqualTo(expected);
        verify(this.userService, times(1)).validateUser(any(User.class));
        verify(this.userService, times(1)).registerUser(any(User.class));
    }

    @Test
    void shouldLoginUser() {
        //Arrange
        LoginRequest loginRequest = new LoginRequest("username", "password");
        User user = new User();
        user.setUsername(loginRequest.username());
        user.setPassword(loginRequest.password());
        user.setRoles(Set.of(new Role(RoleType.ROLE_AUTHOR)));
        SecurityUser securityUser = new SecurityUser(user);
        Authentication expected = new UsernamePasswordAuthenticationToken(
                securityUser,
                null,
                securityUser.getAuthorities());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(
                new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities()));

        //Act
        Authentication actual = underTest.loginUser(loginRequest);

        //Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldThrowUnauthorizedExceptionWhenLoginEmailOrPasswordIsWrong() {
        //Arrange
        LoginRequest request = new LoginRequest("username", "password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenThrow(
                new BadCredentialsException("Username or password is incorrect"));

        //Act Assert
        assertThatThrownBy(() -> underTest.loginUser(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Username or password is incorrect");
    }
}
