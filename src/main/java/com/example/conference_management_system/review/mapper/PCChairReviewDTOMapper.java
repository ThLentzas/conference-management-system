package com.example.conference_management_system.review.mapper;

import com.example.conference_management_system.entity.Review;
import com.example.conference_management_system.review.dto.PCChairReviewDTO;

import java.util.function.Function;

public class PCChairReviewDTOMapper implements Function<Review, PCChairReviewDTO> {

    @Override
    public PCChairReviewDTO apply(Review review) {
        return new PCChairReviewDTO(
                review.getId(),
                review.getPaper().getId(),
                review.getReviewedDate(),
                review.getComment(),
                review.getScore(),
                review.getUser().getFullName()
        );
    }
}
