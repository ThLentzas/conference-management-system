package com.example.conference_management_system.conference.mapper;

import com.example.conference_management_system.conference.dto.ConferenceDTO;
import com.example.conference_management_system.entity.Conference;
import com.example.conference_management_system.user.dto.UserDTO;
import com.example.conference_management_system.user.mapper.UserDTOMapper;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

// https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/convert/converter/Converter.html
import org.springframework.core.convert.converter.Converter;

public class ConferenceDTOMapper implements Converter<Conference, ConferenceDTO> {
    private final UserDTOMapper userDTOMapper = new UserDTOMapper();

    @Override
    public ConferenceDTO convert(Conference conference) {
        Set<UserDTO> users = conference.getConferenceUsers()
                .stream()
                .map(conferenceUser -> this.userDTOMapper.convert(conferenceUser.getUser()))
                .sorted(Comparator.comparing(userDTO -> {
                    // Could not figure out how userDTO could be null, so that userDTO.id() could cause a NullPointerException
                    assert userDTO != null;
                    return userDTO.id();
                }))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return new ConferenceDTO(
                conference.getId(),
                conference.getName(),
                conference.getDescription(),
                users
        );
    }
}
