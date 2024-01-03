package com.example.conference_management_system.paper.mapper;


import com.example.conference_management_system.entity.Paper;
import com.example.conference_management_system.entity.Review;
import com.example.conference_management_system.paper.dto.AuthorPaperDTO;
import com.example.conference_management_system.review.dto.AuthorReviewDTO;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class AuthorPaperDTOMapper implements Function<Paper, AuthorPaperDTO> {

    @Override
    public AuthorPaperDTO apply(Paper paper) {
        Set<AuthorReviewDTO> reviews = new HashSet<>();
        AuthorReviewDTO reviewDTO;

        for (Review review : paper.getReviews()) {
            reviewDTO = new AuthorReviewDTO(
                    review.getId(),
                    review.getPaper().getId(),
                    review.getReviewedDate(),
                    review.getComment(),
                    review.getScore()
            );
            reviews.add(reviewDTO);
        }

        return new AuthorPaperDTO(
                paper.getId(),
                paper.getCreatedDate(),
                paper.getTitle(),
                paper.getAbstractText(),
                paper.getAuthors().split(","),
                paper.getKeywords().split(","),
                paper.getState(),
                reviews
        );
    }
}
