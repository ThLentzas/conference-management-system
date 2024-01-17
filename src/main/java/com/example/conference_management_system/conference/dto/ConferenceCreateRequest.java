package com.example.conference_management_system.conference.dto;

import jakarta.validation.constraints.NotBlank;

public record ConferenceCreateRequest(
        @NotBlank(message = "You must provide the name of the conference")
        String name,
        @NotBlank(message = "You must provide the description of the conference")
        String description) {
}
