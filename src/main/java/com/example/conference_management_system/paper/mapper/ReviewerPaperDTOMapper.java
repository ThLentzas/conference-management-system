package com.example.conference_management_system.paper.mapper;

import com.example.conference_management_system.entity.Paper;
import com.example.conference_management_system.entity.Review;
import com.example.conference_management_system.paper.dto.ReviewerPaperDTO;
import com.example.conference_management_system.review.dto.ReviewDTO;
import com.example.conference_management_system.review.dto.ReviewerReviewDTO;
import com.example.conference_management_system.review.mapper.ReviewerReviewDTOMapper;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

// https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/convert/converter/Converter.html
import org.springframework.core.convert.converter.Converter;

public class ReviewerPaperDTOMapper implements Converter<Paper, ReviewerPaperDTO> {
    private final ReviewerReviewDTOMapper reviewerReviewDTOMapper = new ReviewerReviewDTOMapper();

    @Override
    public ReviewerPaperDTO convert(Paper paper) {
        Set<ReviewerReviewDTO> reviews = new HashSet<>();

        for (Review review : paper.getReviews()) {
            reviews.add(this.reviewerReviewDTOMapper.apply(review));
        }

        reviews = reviews.stream()
                .sorted(Comparator.comparing(ReviewDTO::getReviewedDate))
                .collect(Collectors.toCollection(LinkedHashSet::new));

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
