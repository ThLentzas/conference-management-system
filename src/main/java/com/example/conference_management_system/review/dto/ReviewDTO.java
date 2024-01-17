package com.example.conference_management_system.review.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class ReviewDTO {
    private Long id;
    private Long paperId;
    private LocalDate reviewedDate;
    private String comment;
    private Double score;
}
