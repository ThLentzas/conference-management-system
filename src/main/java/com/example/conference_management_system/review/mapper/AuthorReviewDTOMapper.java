package com.example.conference_management_system.review.mapper;

import com.example.conference_management_system.entity.Review;
import com.example.conference_management_system.review.dto.AuthorReviewDTO;

import java.util.function.Function;

public class AuthorReviewDTOMapper implements Function<Review, AuthorReviewDTO> {

    @Override
    public AuthorReviewDTO apply(Review review) {
        return new AuthorReviewDTO(
                review.getId(),
                review.getPaper().getId(),
                review.getReviewedDate(),
                review.getComment(),
                review.getScore()
        );
    }
}
