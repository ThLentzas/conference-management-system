package com.example.conference_management_system.auth;

import jakarta.validation.constraints.NotBlank;

record LoginRequest(
        @NotBlank(message = "The username field is required")
        String username,
        @NotBlank(message = "The password field is required")
        String password
) {
}
