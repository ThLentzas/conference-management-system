package com.example.conference_management_system.conference.mapper;

import com.example.conference_management_system.conference.dto.PCChairConferenceDTO;
import com.example.conference_management_system.entity.Conference;
import com.example.conference_management_system.paper.dto.PCChairPaperDTO;
import com.example.conference_management_system.paper.mapper.PCChairPaperDTOMapper;
import com.example.conference_management_system.user.dto.UserDTO;
import com.example.conference_management_system.user.mapper.UserDTOMapper;

import java.util.Set;
import java.util.stream.Collectors;

// https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/convert/converter/Converter.html
import org.springframework.core.convert.converter.Converter;

public class PCChairConferenceDTOMapper implements Converter<Conference, PCChairConferenceDTO> {
    private final UserDTOMapper userDTOMapper = new UserDTOMapper();
    private final PCChairPaperDTOMapper pcChairPaperDTOMapper = new PCChairPaperDTOMapper();

    @Override
    public PCChairConferenceDTO convert(Conference conference) {
        Set<UserDTO> users = conference.getConferenceUsers()
                .stream()
                .map(conferenceUser -> this.userDTOMapper.convert(conferenceUser.getUser()))
                .collect(Collectors.toSet());

        Set<PCChairPaperDTO> papers = conference.getPapers()
                .stream()
                .map(this.pcChairPaperDTOMapper::convert)
                .collect(Collectors.toSet());

        return new PCChairConferenceDTO(
                conference.getId(),
                conference.getName(),
                conference.getDescription(),
                users,
                conference.getState(),
                papers
        );
    }
}
