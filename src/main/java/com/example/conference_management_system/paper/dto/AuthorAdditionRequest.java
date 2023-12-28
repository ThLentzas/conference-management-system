package com.example.conference_management_system.paper.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthorAdditionRequest(@NotBlank(message = "You must provide the author's name")
                             String author) {
}
