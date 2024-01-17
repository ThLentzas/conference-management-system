package com.example.conference_management_system.user.dto;

import jakarta.validation.constraints.NotNull;

public record ReviewerAssignmentRequest(
        @NotNull (message = "You must provide the id of the user to be added as reviewer")
        Long userId
) {
}
