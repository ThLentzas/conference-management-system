package com.example.conference_management_system.user.dto;

import com.example.conference_management_system.role.RoleType;

import java.util.Set;

/*
    We don't want to return a Set<Role> because that way we expose our entity
 */
public record UserDTO(Long id, String username, String fullName, Set<RoleType> roleTypes) {
}
