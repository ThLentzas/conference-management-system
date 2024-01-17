package com.example.conference_management_system.paper.dto;

import lombok.Getter;
import lombok.Setter;

import com.example.conference_management_system.paper.PaperState;
import com.example.conference_management_system.review.dto.PCChairReviewDTO;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
public class PCChairPaperDTO extends PaperDTO {
    private PaperState state;
    private Set<PCChairReviewDTO> reviews;

    public PCChairPaperDTO(Long id,
            LocalDate createdDate,
            String title,
            String abstractText,
            String[] authors,
            String[] keywords,
            PaperState state,
            Set<PCChairReviewDTO> reviews
    ) {
        super(id, createdDate, title, abstractText, authors, keywords);
        this.state = state;
        this.reviews = reviews;
    }
}
