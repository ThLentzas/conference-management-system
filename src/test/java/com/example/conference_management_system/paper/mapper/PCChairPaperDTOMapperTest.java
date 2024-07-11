package com.example.conference_management_system.paper.mapper;

import com.example.conference_management_system.entity.Paper;
import com.example.conference_management_system.entity.Review;
import com.example.conference_management_system.entity.User;
import com.example.conference_management_system.paper.PaperState;
import com.example.conference_management_system.paper.dto.PCChairPaperDTO;
import com.example.conference_management_system.review.dto.PCChairReviewDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PCChairPaperDTOMapperTest {
    private PCChairPaperDTOMapper underTest;

    @BeforeEach
    void setup() {
        underTest = new PCChairPaperDTOMapper();
    }

    @Test
    void shouldMapPaperToPCChairPaperDTO() {
        String[] authors = {"author 1, author 2"};
        String[] keywords = {"keyword 1, keyword 2"};

        User user = new User();
        user.setFullName("full name");

        Paper paper = new Paper();
        paper.setId(1L);
        paper.setCreatedDate(LocalDate.now().minusDays(2));
        paper.setTitle("title");
        paper.setAbstractText("abstractText");
        paper.setAuthors(String.join(",", authors));
        paper.setKeywords(String.join(",", keywords));

        Review review = new Review();
        review.setId(1L);
        review.setReviewedDate(LocalDate.now());
        review.setUser(user);
        review.setPaper(paper);
        review.setComment("comment");
        review.setScore(9.1);
        paper.setReviews(Set.of(review));

        PCChairReviewDTO pcChairReviewDTO = new PCChairReviewDTO(1L, 1L, LocalDate.now(), "comment", 9.1, "full name");

        PCChairPaperDTO expected = new PCChairPaperDTO(
                1L,
                LocalDate.now().minusDays(2),
                "title",
                "abstractText",
                authors,
                keywords,
                PaperState.CREATED,
                Set.of(pcChairReviewDTO)
        );

        PCChairPaperDTO actual = this.underTest.convert(paper);

        assertThat(actual).isEqualTo(expected);
    }
}
