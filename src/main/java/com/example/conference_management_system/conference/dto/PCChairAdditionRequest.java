package com.example.conference_management_system.conference.dto;

import jakarta.validation.constraints.NotNull;

public record PCChairAdditionRequest(
        @NotNull(message = "You must provide the id of the user to be added as PCChair")
        Long userId
) {
}
