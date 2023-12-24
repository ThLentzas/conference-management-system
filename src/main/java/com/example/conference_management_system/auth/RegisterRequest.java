package com.example.conference_management_system.auth;

import com.example.conference_management_system.role.RoleType;

import jakarta.validation.constraints.NotBlank;

import java.util.HashSet;
import java.util.Set;

record RegisterRequest(
        @NotBlank(message = "The username field is required")
        String username,
        @NotBlank(message = "The password field is required")
        String password,
        @NotBlank(message = "The full name field is required")
        String fullName,
        Set<RoleType> roleTypes
) {
    /*
        If the roleTypes are not present in the register request an empty set of roles is assigned to that user.
     */
    public RegisterRequest {
        if (roleTypes == null) {
            roleTypes = new HashSet<>();
        }
    }
}
