package com.example.conference_management_system.conference;

import com.example.conference_management_system.AbstractUnitTest;
import com.example.conference_management_system.entity.Conference;
import com.example.conference_management_system.entity.ConferenceUser;
import com.example.conference_management_system.entity.User;
import com.example.conference_management_system.entity.key.ConferenceUserId;
import com.example.conference_management_system.user.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class ConferenceUserRepositoryTest extends AbstractUnitTest {
    @Autowired
    private ConferenceRepository conferenceRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ConferenceUserRepository underTest;

    //existsByConferenceIdAndUserId()
    @Test
    void shouldReturnTrueWhenUserIsPcChairAtConference() {
        Conference conference = getConference();
        User user = getUser();
        this.conferenceRepository.save(conference);
        this.userRepository.save(user);
        ConferenceUser conferenceUser = new ConferenceUser(
                new ConferenceUserId(conference.getId(), user.getId()),
                conference,
                user
        );
        this.underTest.save(conferenceUser);

        assertThat(this.underTest.existsByConferenceIdAndUserId(conference.getId(), user.getId())).isTrue();
    }

    @Test
    void shouldReturnFalseWhenUserIsNotPcChairAtConference() {
        Conference conference = getConference();
        User user = getUser();
        this.conferenceRepository.save(conference);
        this.userRepository.save(user);
        ConferenceUser conferenceUser = new ConferenceUser(
                new ConferenceUserId(conference.getId(), user.getId()),
                conference,
                user
        );
        this.underTest.save(conferenceUser);

        assertThat(this.underTest.existsByConferenceIdAndUserId(conference.getId(), user.getId() + 1L)).isFalse();
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
