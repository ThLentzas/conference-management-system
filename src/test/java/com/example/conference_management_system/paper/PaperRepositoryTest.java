package com.example.conference_management_system.paper;

import com.example.conference_management_system.AbstractUnitTest;
import com.example.conference_management_system.entity.Paper;
import com.example.conference_management_system.entity.User;
import com.example.conference_management_system.user.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PaperRepositoryTest extends AbstractUnitTest {
    @Autowired
    private PaperRepository underTest;
    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldReturnTrueWhenSearchingForAPaperThatExistsWithGivenTitleIgnoringCase() {
        //Arrange
        Paper paper = getPaper();
        this.underTest.save(paper);

        //Act & Assert
        assertThat(this.underTest.existsByTitleIgnoreCase("tiTle")).isTrue();
    }

    @Test
    void shouldReturnFalseWhenSearchingForAPaperThatDoesNotExistWithGivenTitleIgnoringCase() {
        //Arrange
        Paper paper = getPaper();
        this.underTest.save(paper);

        //Act & Assert
        assertThat(this.underTest.existsByTitleIgnoreCase("test")).isFalse();
    }

    @Test
    void shouldReturnTrueWhenUserIsAuthorOfPaper() {
        User user = getUser();
        Paper paper = getPaper();
        paper.setUsers(Set.of(user));

        this.userRepository.save(user);
        this.underTest.save(paper);

        assertThat(this.underTest.isAuthorAtPaper(paper.getId(), user.getId())).isTrue();
    }

    @Test
    void shouldReturnFalseWhenUserIsNotAuthorOfPaper() {
        User user = getUser();
        Paper paper = getPaper();
        paper.setUsers(Set.of(user));

        this.userRepository.save(user);
        this.underTest.save(paper);

        assertThat(this.underTest.isAuthorAtPaper(paper.getId(), 2L)).isFalse();
    }

    private Paper getPaper() {
        Paper paper = new Paper();
        paper.setTitle("title");
        paper.setAbstractText("abstractText");
        paper.setAuthors("author 1, author2");
        paper.setKeywords("keyword 1, keyword 2");

        return paper;
    }

    private User getUser() {
        User user = new User();
        user.setUsername("username");
        user.setPassword("password");
        user.setFullName("full name");

        return user;
    }
}
