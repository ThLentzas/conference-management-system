package com.example.conference_management_system.review.mapper;

import com.example.conference_management_system.entity.Paper;
import com.example.conference_management_system.entity.Review;
import com.example.conference_management_system.entity.User;
import com.example.conference_management_system.review.dto.AuthorReviewDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorReviewDTOMapperTest {
    private AuthorReviewDTOMapper underTest;

    @BeforeEach
    void setup() {
        underTest = new AuthorReviewDTOMapper();
    }

    @Test
    void shouldMapReviewToAuthorReviewDTO() {
        Paper paper = new Paper();
        paper.setId(1L);
        User user = new User();

        Review review = new Review();
        review.setId(1L);
        review.setReviewedDate(LocalDate.now());
        review.setUser(user);
        review.setPaper(paper);
        review.setComment("comment");
        review.setScore(9.1);

        AuthorReviewDTO expected = new AuthorReviewDTO(1L, 1L, LocalDate.now(), "comment", 9.1);

        AuthorReviewDTO actual = this.underTest.apply(review);

        assertThat(actual).isEqualTo(expected);
    }
}
