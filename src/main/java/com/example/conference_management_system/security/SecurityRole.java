package com.example.conference_management_system.security;

import com.example.conference_management_system.entity.Role;
import org.springframework.security.core.GrantedAuthority;

public record SecurityRole(Role role) implements GrantedAuthority {

    @Override
    public String getAuthority() {
        return role.getType().name();
    }
}
