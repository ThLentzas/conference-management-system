package com.example.conference_management_system.conference.mapper;

import com.example.conference_management_system.conference.dto.ConferenceDTO;
import com.example.conference_management_system.entity.Conference;
import com.example.conference_management_system.user.UserDTO;
import com.example.conference_management_system.user.UserDTOMapper;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConferenceDTOMapper implements Function<Conference, ConferenceDTO> {
    private final UserDTOMapper userDTOMapper = new UserDTOMapper();

    @Override
    public ConferenceDTO apply(Conference conference) {
        Set<UserDTO> users = conference.getUsers()
                .stream()
                .map(this.userDTOMapper)
                .collect(Collectors.toSet());

        return new ConferenceDTO(
                conference.getId(),
                conference.getName(),
                conference.getDescription(),
                users
        );
    }
}
