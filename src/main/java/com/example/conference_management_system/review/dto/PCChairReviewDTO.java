package com.example.conference_management_system.review.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PCChairReviewDTO extends ReviewDTO {
    private LocalDate assignedDate;
    private String reviewer;

    public PCChairReviewDTO(
            Long id,
            Long paperId,
            LocalDate reviewedDate,
            String comment,
            Double score,
            String reviewer
    ) {
        super(id, paperId, reviewedDate, comment, score);
        this.reviewer = reviewer;
    }
}
