package com.example.conference_management_system.conference.mapper;

import com.example.conference_management_system.conference.ConferenceState;
import com.example.conference_management_system.conference.dto.PCChairConferenceDTO;
import com.example.conference_management_system.entity.Conference;
import com.example.conference_management_system.entity.ConferenceUser;
import com.example.conference_management_system.entity.Paper;
import com.example.conference_management_system.entity.Review;
import com.example.conference_management_system.entity.Role;
import com.example.conference_management_system.entity.User;
import com.example.conference_management_system.entity.key.ConferenceUserId;
import com.example.conference_management_system.paper.PaperState;
import com.example.conference_management_system.paper.dto.PCChairPaperDTO;
import com.example.conference_management_system.review.dto.PCChairReviewDTO;
import com.example.conference_management_system.role.RoleType;
import com.example.conference_management_system.user.dto.UserDTO;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PCChairConferenceDTOMapperTest {
    private PCChairConferenceDTOMapper underTest;

    @BeforeEach
    void setup() {
        underTest = new PCChairConferenceDTOMapper();
    }

    @Test
    void shouldMapConferenceToPCChairConferenceDTO() {
        String[] authors = {"author 1, author 2"};
        String[] keywords = {"keyword 1, keyword 2"};

        User user = new User();
        user.setId(1L);
        user.setUsername("username");
        user.setFullName("full name");
        user.setRoles(Set.of(new Role(RoleType.ROLE_REVIEWER), new Role(RoleType.ROLE_PC_CHAIR)));

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
        PCChairPaperDTO pcChairPaperDTO = new PCChairPaperDTO(
                1L,
                LocalDate.now().minusDays(2),
                "title",
                "abstractText",
                authors,
                keywords,
                PaperState.CREATED,
                Set.of(pcChairReviewDTO)
        );
        UserDTO userDTO = new UserDTO(
                1L,
                "username",
                "full name",
                Set.of(RoleType.ROLE_REVIEWER, RoleType.ROLE_PC_CHAIR)
        );

        UUID conferenceId = UUID.randomUUID();
        Conference conference = new Conference();
        conference.setId(conferenceId);
        conference.setName("name");
        conference.setDescription("description");
        conference.setPapers(Set.of(paper));

        ConferenceUser conferenceUser = new ConferenceUser(
                new ConferenceUserId(conferenceId, user.getId()),
                conference,
                user
        );
        conference.setConferenceUsers(Set.of(conferenceUser));

        PCChairConferenceDTO expected = new PCChairConferenceDTO(
                conferenceId,
                "name",
                "description",
                Set.of(userDTO),
                ConferenceState.CREATED,
                Set.of(pcChairPaperDTO)
        );

        PCChairConferenceDTO actual = this.underTest.apply(conference);

        assertThat(actual).isEqualTo(expected);
    }
}
