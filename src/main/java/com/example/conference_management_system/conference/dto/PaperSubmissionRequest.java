package com.example.conference_management_system.conference.dto;

import jakarta.validation.constraints.NotNull;

public record PaperSubmissionRequest(@NotNull Long paperId) {
}
