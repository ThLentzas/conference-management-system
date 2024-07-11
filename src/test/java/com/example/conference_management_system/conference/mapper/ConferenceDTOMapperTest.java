package com.example.conference_management_system.conference.mapper;

import com.example.conference_management_system.conference.dto.ConferenceDTO;
import com.example.conference_management_system.entity.Conference;
import com.example.conference_management_system.entity.ConferenceUser;
import com.example.conference_management_system.entity.Role;
import com.example.conference_management_system.entity.User;
import com.example.conference_management_system.entity.key.ConferenceUserId;
import com.example.conference_management_system.role.RoleType;
import com.example.conference_management_system.user.dto.UserDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConferenceDTOMapperTest {
    private ConferenceDTOMapper underTest;

    @BeforeEach
    void setup() {
        underTest = new ConferenceDTOMapper();
    }

    @Test
    void shouldMapConferenceToConferenceDTO() {
        UUID conferenceId = UUID.randomUUID();
        Conference conference = new Conference();
        conference.setId(conferenceId);
        conference.setName("name");
        conference.setDescription("description");

        User user = new User();
        user.setId(1L);
        user.setUsername("username");
        user.setFullName("full name");
        user.setRoles(Set.of(new Role(RoleType.ROLE_PC_CHAIR)));

        ConferenceUser conferenceUser = new ConferenceUser(
                new ConferenceUserId(conferenceId, user.getId()),
                conference,
                user
        );
        conference.setConferenceUsers(Set.of(conferenceUser));
        UserDTO userDTO = new UserDTO(1L, "username", "full name", Set.of(RoleType.ROLE_PC_CHAIR));

        ConferenceDTO expected = new ConferenceDTO(
                conferenceId,
                "name",
                "description",
                Set.of(userDTO)
        );

        ConferenceDTO actual = this.underTest.convert(conference);

        assertThat(actual).isEqualTo(expected);

    }
}
