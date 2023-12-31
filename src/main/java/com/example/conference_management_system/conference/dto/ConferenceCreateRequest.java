package com.example.conference_management_system.conference.dto;

import jakarta.validation.constraints.NotBlank;

public record ConferenceCreateRequest(
        @NotBlank
        String name,
        @NotBlank
        String description) {
}
