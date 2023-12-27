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
import com.example.conference_management_system.entity.Role;
import com.example.conference_management_system.role.RoleRepository;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RoleRepository roleRepository;

    Authentication registerUser(RegisterRequest request) {
        Set<Role> tmp = request.roleTypes().stream()
                .map(Role::new)
                .collect(Collectors.toSet());

        /*
            Setting the roles of the request to the user and then creating the user would result in:
            object references an unsaved transient instance - save the transient instance before flushing:
            com.example.conference_management_system.entity.Role

            We have to save the roles first and then save the user. In our case when we call findByType() the entity
            is added to context, and then we can call save.
         */
        Set<Role> roles = new HashSet<>();
        for (Role role : tmp) {
            this.roleRepository.findByType(role.getType()).ifPresent(roles::add);
        }

        User user = new User(request.username(), request.password(), request.fullName(), roles);
        this.userService.validateUser(user);
        user.setPassword(this.passwordEncoder.encode(user.getPassword()));
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

    public void invalidateSession(HttpServletRequest servletRequest) {
        HttpSession session = servletRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}