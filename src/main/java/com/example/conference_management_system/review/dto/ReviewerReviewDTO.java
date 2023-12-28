package com.example.conference_management_system.review.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ReviewerReviewDTO extends ReviewDTO {
    private String reviewer;

    public ReviewerReviewDTO(
            Long id,
            Long paperId,
            LocalDate createdDate,
            String comment,
            Double score,
            String reviewer
    ) {
        super(id, paperId, createdDate, comment, score);
        this.reviewer = reviewer;
    }
}