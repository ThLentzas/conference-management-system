package com.example.conference_management_system.paper.dto;

import jakarta.validation.constraints.NotNull;

public record AuthorAdditionRequest(
        @NotNull(message = "You must provide the id of the author")
        Long userId
) {
}
