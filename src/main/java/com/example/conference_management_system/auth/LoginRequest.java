package com.example.conference_management_system.auth;

import jakarta.validation.constraints.NotBlank;

record LoginRequest(
        @NotBlank(message = "The Username field is necessary")
        String username,
        @NotBlank(message = "The Password field is necessary")
        String password) {
}
