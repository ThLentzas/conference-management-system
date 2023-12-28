package com.example.conference_management_system.user;

import com.example.conference_management_system.role.RoleType;

import java.util.Set;

record UserDTO(String username, String fullName, Set<RoleType> roles) {
}
