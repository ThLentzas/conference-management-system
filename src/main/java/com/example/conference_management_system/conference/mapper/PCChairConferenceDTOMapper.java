package com.example.conference_management_system.conference.mapper;

import com.example.conference_management_system.conference.dto.PCChairConferenceDTO;
import com.example.conference_management_system.entity.Conference;
import com.example.conference_management_system.paper.dto.PCChairPaperDTO;
import com.example.conference_management_system.paper.mapper.PCChairPaperDTOMapper;
import com.example.conference_management_system.user.dto.UserDTO;
import com.example.conference_management_system.user.UserDTOMapper;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PCChairConferenceDTOMapper implements Function<Conference, PCChairConferenceDTO> {
    private final UserDTOMapper userDTOMapper = new UserDTOMapper();
    private final PCChairPaperDTOMapper pcChairPaperDTOMapper = new PCChairPaperDTOMapper();

    @Override
    public PCChairConferenceDTO apply(Conference conference) {
        Set<UserDTO> users = conference.getConferenceUsers()
                .stream()
                .map(conferenceUser -> this.userDTOMapper.apply(conferenceUser.getUser()))
                .collect(Collectors.toSet());

        Set<PCChairPaperDTO> papers = conference.getPapers().stream()
                .map(this.pcChairPaperDTOMapper)
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
