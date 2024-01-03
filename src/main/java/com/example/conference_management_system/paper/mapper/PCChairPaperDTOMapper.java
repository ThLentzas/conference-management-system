package com.example.conference_management_system.paper.mapper;

import com.example.conference_management_system.entity.Paper;
import com.example.conference_management_system.entity.Review;
import com.example.conference_management_system.paper.dto.PCChairPaperDTO;
import com.example.conference_management_system.review.dto.PCChairReviewDTO;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class PCChairPaperDTOMapper implements Function<Paper, PCChairPaperDTO> {

    @Override
    public PCChairPaperDTO apply(Paper paper) {
        Set<PCChairReviewDTO> reviews = new HashSet<>();
        PCChairReviewDTO reviewDTO;

        for (Review review : paper.getReviews()) {
            reviewDTO = new PCChairReviewDTO(
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

        return new PCChairPaperDTO(
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
