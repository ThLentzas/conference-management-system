package com.example.conference_management_system.paper.dto;

import com.example.conference_management_system.paper.PaperState;
import com.example.conference_management_system.review.dto.AuthorReviewDTO;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
public class AuthorPaperDTO extends PaperDTO {
    private PaperState state;
    private Set<AuthorReviewDTO> reviews;

    public AuthorPaperDTO(
            Long id,
            LocalDate createdDate,
            String title,
            String abstractText,
            String[] authors, String[] keywords,
            PaperState state,
            Set<AuthorReviewDTO> reviews
    ) {
        super(id, createdDate, title, abstractText, authors, keywords);
        this.state = state;
        this.reviews = reviews;
    }
}
