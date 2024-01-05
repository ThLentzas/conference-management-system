package com.example.conference_management_system.conference;

import com.example.conference_management_system.AbstractUnitTest;
import com.example.conference_management_system.entity.Conference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class ConferenceRepositoryTest extends AbstractUnitTest {
    @Autowired
    private ConferenceRepository underTest;

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

    private Conference getConference() {
        Conference conference = new Conference();
        conference.setName("conference");
        conference.setDescription("description");

        return conference;
    }
}
