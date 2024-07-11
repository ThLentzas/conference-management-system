package com.example.conference_management_system.paper.mapper;

import com.example.conference_management_system.entity.Paper;
import com.example.conference_management_system.entity.Review;
import com.example.conference_management_system.entity.User;
import com.example.conference_management_system.paper.PaperState;
import com.example.conference_management_system.paper.dto.AuthorPaperDTO;
import com.example.conference_management_system.review.dto.AuthorReviewDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AuthorPaperDTOMapperTest {
    private AuthorPaperDTOMapper underTest;

    @BeforeEach
    void setup() {
        underTest = new AuthorPaperDTOMapper();
    }

    @Test
    void shouldMapPaperToAuthorPaperDTO() {
        String[] authors = {"author 1, author 2"};
        String[] keywords = {"keyword 1, keyword 2"};

        User user = new User();
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

        AuthorReviewDTO authorReviewDTO = new AuthorReviewDTO(1L, 1L, LocalDate.now(), "comment", 9.1);

        AuthorPaperDTO expected = new AuthorPaperDTO(
                1L,
                LocalDate.now().minusDays(2),
                "title",
                "abstractText",
                authors,
                keywords,
                PaperState.CREATED,
                Set.of(authorReviewDTO)
        );

        AuthorPaperDTO actual = this.underTest.convert(paper);

        assertThat(actual).isEqualTo(expected);
    }
}
