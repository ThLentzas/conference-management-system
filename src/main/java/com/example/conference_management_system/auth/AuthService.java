package com.example.conference_management_system.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.conference_management_system.entity.User;
import com.example.conference_management_system.exception.UnauthorizedException;
import com.example.conference_management_system.security.SecurityUser;
import com.example.conference_management_system.user.UserService;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
class AuthService {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    Authentication registerUser(RegisterRequest request) {
        User user = new User(request.username(), request.password(), request.fullName());
        this.userService.validateUser(user);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user = this.userService.registerUser(user);

        SecurityUser securityUser = new SecurityUser(user);

        return new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
    }

    Authentication loginUser(LoginRequest request) {
        Authentication authentication;

        try {
            authentication = this.authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    request.username(),
                    request.password()));
        } catch (BadCredentialsException bce) {
            throw new UnauthorizedException("Username or password is incorrect");
        }

        return authentication;
    }
}