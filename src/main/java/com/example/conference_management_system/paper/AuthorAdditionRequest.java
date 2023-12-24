package com.example.conference_management_system.paper;

import jakarta.validation.constraints.NotBlank;

record AuthorAdditionRequest(@NotBlank(message = "You must provide the author's name")
                             String author) {
}
