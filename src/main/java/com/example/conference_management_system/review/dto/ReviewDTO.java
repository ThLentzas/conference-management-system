package com.example.conference_management_system.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
public class ReviewDTO {
    private Long id;
    private Long paperId;
    private LocalDate createdDate;
    private String comment;
    private Double score;
}
