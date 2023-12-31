package com.example.conference_management_system.conference;

import com.example.conference_management_system.AbstractUnitTest;
import com.example.conference_management_system.entity.Conference;
import com.example.conference_management_system.entity.User;
import com.example.conference_management_system.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ConferenceRepositoryTest extends AbstractUnitTest {
    @Autowired
    private ConferenceRepository underTest;
    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldReturnTrueWhenSearchingForAConferenceThatExistsWithGivenNameIgnoringCase() {
        Conference conference = getConference();
        this.underTest.save(conference);

        assertThat(this.underTest.existsByNameIgnoringCase("ConfereNce")).isTrue();
    }

    @Test
    void shouldReturnFalseWhenSearchingForAConferenceThatDoesNotExistsWithGivenNameIgnoringCase() {
        Conference conference = getConference();
        this.underTest.save(conference);

        assertThat(this.underTest.existsByNameIgnoringCase("test")).isFalse();
    }

    @Test
    void shouldReturnTrueWhenUserIsPc_ChairAtConference() {
        Conference conference = getConference();
        User user = getUser();
        conference.setUsers(Set.of(user));

        this.userRepository.save(user);
        this.underTest.save(conference);

        assertThat(this.underTest.isPc_ChairAtConference(conference.getId(), user.getId())).isTrue();
    }

    @Test
    void shouldReturnFalseWhenUserIsNotPc_ChairAtConference() {
        Conference conference = getConference();
        User user = getUser();
        conference.setUsers(Set.of(user));

        this.userRepository.save(user);
        this.underTest.save(conference);

        assertThat(this.underTest.isPc_ChairAtConference(conference.getId(), 2L)).isFalse();
    }

    private Conference getConference() {
        Conference conference = new Conference();
        conference.setName("conference");
        conference.setDescription("description");

        return conference;
    }

    private User getUser() {
        User user = new User();
        user.setUsername("username");
        user.setPassword("password");
        user.setFullName("full name");

        return user;
    }
}
