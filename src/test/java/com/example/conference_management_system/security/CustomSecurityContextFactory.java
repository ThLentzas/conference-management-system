package com.example.conference_management_system.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import com.example.conference_management_system.entity.Role;
import com.example.conference_management_system.entity.User;
import com.example.conference_management_system.role.RoleType;

import java.util.Set;

public class CustomSecurityContextFactory implements WithSecurityContextFactory<WithMockCustomUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockCustomUser annotation) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        User user = new User();
        user.setUsername(annotation.username());
        user.setPassword(annotation.password());
        user.setRoles(Set.of(new Role(RoleType.valueOf(annotation.roles()[0]))));

        SecurityUser principal = new SecurityUser(user);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
        securityContext.setAuthentication(authentication);

        return securityContext;
    }
}
