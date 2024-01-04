package com.example.conference_management_system.review.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReviewCreateRequest(
        @NotNull
        @DecimalMin(value = "0.0", message = "Score must be in the range of [0.0 - 10.0]")
        @DecimalMax(value = "10.0", message = "Score must be in the range of [0.0 - 10.0]")
        Double score,
        @NotBlank String comment
) {
}
