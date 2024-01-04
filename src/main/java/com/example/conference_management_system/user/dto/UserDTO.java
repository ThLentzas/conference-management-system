package com.example.conference_management_system.user.dto;

import com.example.conference_management_system.role.RoleType;

import java.util.Set;

public record UserDTO(Long id, String username, String fullName, Set<RoleType> roles) {
}
