package com.example.conference_management_system.review.dto;

import java.time.LocalDate;

public class AuthorReviewDTO extends ReviewDTO {
    public AuthorReviewDTO(Long id, Long paperId, LocalDate createdDate, String comment, Double score) {
        super(id, paperId, createdDate, comment, score);
    }
}
