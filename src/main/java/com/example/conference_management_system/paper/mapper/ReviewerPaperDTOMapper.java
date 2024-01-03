package com.example.conference_management_system.paper.mapper;

import com.example.conference_management_system.entity.Paper;
import com.example.conference_management_system.entity.Review;
import com.example.conference_management_system.paper.dto.ReviewerPaperDTO;
import com.example.conference_management_system.review.dto.ReviewerReviewDTO;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class ReviewerPaperDTOMapper implements Function<Paper, ReviewerPaperDTO> {

    @Override
    public ReviewerPaperDTO apply(Paper paper) {
        Set<ReviewerReviewDTO> reviews = new HashSet<>();
        ReviewerReviewDTO reviewDTO;

        for (Review review : paper.getReviews()) {
            reviewDTO = new ReviewerReviewDTO(
                    review.getId(),
                    review.getPaper().getId(),
                    review.getAssignedDate(),
                    review.getReviewedDate(),
                    review.getComment(),
                    review.getScore(),
                    review.getUser().getUsername()
            );
            reviews.add(reviewDTO);
        }

        return new ReviewerPaperDTO(
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
