package com.example.conference_management_system.review.dto;

import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
public class AuthorReviewDTO extends ReviewDTO {

    public AuthorReviewDTO(Long id, Long paperId, LocalDate reviewedDate, String comment, Double score) {
        super(id, paperId, reviewedDate, comment, score);
    }
}
