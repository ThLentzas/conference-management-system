package com.example.conference_management_system.paper.mapper;

import com.example.conference_management_system.entity.Paper;
import com.example.conference_management_system.entity.Review;
import com.example.conference_management_system.paper.dto.PCChairPaperDTO;
import com.example.conference_management_system.review.dto.PCChairReviewDTO;
import com.example.conference_management_system.review.dto.ReviewDTO;
import com.example.conference_management_system.review.mapper.PCChairReviewDTOMapper;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

// https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/convert/converter/Converter.html
import org.springframework.core.convert.converter.Converter;

public class PCChairPaperDTOMapper implements Converter<Paper, PCChairPaperDTO> {
    private final PCChairReviewDTOMapper pcChairReviewDTOMapper = new PCChairReviewDTOMapper();

    @Override
    public PCChairPaperDTO convert(Paper paper) {
        Set<PCChairReviewDTO> reviews = new HashSet<>();

        for (Review review : paper.getReviews()) {
            reviews.add(this.pcChairReviewDTOMapper.apply(review));
        }

        reviews = reviews.stream()
                .sorted(Comparator.comparing(ReviewDTO::getReviewedDate))
                .collect(Collectors.toCollection(LinkedHashSet::new));

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
