package com.example.conference_management_system.review.mapper;

import com.example.conference_management_system.entity.Review;
import com.example.conference_management_system.review.dto.ReviewerReviewDTO;

import java.util.function.Function;

public class ReviewerReviewDTOMapper implements Function<Review, ReviewerReviewDTO> {

    @Override
    public ReviewerReviewDTO apply(Review review) {
        return new ReviewerReviewDTO(
                review.getId(),
                review.getPaper().getId(),
                review.getReviewedDate(),
                review.getComment(),
                review.getScore(),
                review.getUser().getFullName()
        );
    }
}
