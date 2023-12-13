package com.example.conference_management_system.auth;

import jakarta.validation.constraints.NotBlank;

record RegisterRequest(
        @NotBlank(message = "The username field is required")
        String username,
        @NotBlank(message = "The password field is required")
        String password,
        @NotBlank(message = "The full name field is required")
        String fullName) {
}
