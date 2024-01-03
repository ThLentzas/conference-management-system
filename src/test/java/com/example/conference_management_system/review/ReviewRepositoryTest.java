package com.example.conference_management_system.review;

import com.example.conference_management_system.AbstractUnitTest;
import com.example.conference_management_system.entity.Paper;
import com.example.conference_management_system.entity.Review;
import com.example.conference_management_system.entity.Role;
import com.example.conference_management_system.entity.User;
import com.example.conference_management_system.paper.PaperRepository;
import com.example.conference_management_system.role.RoleRepository;
import com.example.conference_management_system.role.RoleType;
import com.example.conference_management_system.user.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewRepositoryTest extends AbstractUnitTest {
    @Autowired
    private PaperRepository paperRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private ReviewRepository underTest;

    @Test
    void shouldReturnTrueWhenUserIsReviewerAtPaper() {
        Paper paper = getPaper();
        User user = getUser();
        Role role = new Role(RoleType.ROLE_REVIEWER);
        user.setRoles(Set.of(role));
        Review review = new Review(paper, user);

        this.roleRepository.save(role);
        this.userRepository.save(user);
        this.paperRepository.save(paper);
        this.underTest.save(review);

        assertThat(this.underTest.isReviewerAtPaper(paper.getId(), user.getId())).isTrue();
    }

    @Test
    void shouldReturnFalseWhenUserIsReviewerAtPaper() {
        Paper paper = getPaper();
        User user = getUser();
        Role role = new Role(RoleType.ROLE_REVIEWER);
        user.setRoles(Set.of(role));
        Review review = new Review(paper, user);

        this.roleRepository.save(role);
        this.userRepository.save(user);
        this.paperRepository.save(paper);
        this.underTest.save(review);

        assertThat(this.underTest.isReviewerAtPaper(paper.getId(), user.getId() + 1)).isFalse();
    }

    @Test
    void shouldReturnListOfReviewsForFindByPaperId() {
        Paper paper = getPaper();
        User user = getUser();
        Review review = new Review(paper, user);

        this.userRepository.save(user);
        this.paperRepository.save(paper);
        this.underTest.save(review);

        assertThat(this.underTest.findByPaperId(paper.getId())).hasSize(1);
    }

    private Paper getPaper() {
        Paper paper = new Paper();
        paper.setTitle("title");
        paper.setAbstractText("abstract text");
        paper.setAuthors("author");
        paper.setKeywords("keyword");

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
